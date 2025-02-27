package org.cenva.parser

import arrow.core.Either

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.raise.catch
import jakarta.enterprise.context.ApplicationScoped

import org.cenva.dao.sip.*



import org.jboss.logging.Logger;
import kotlin.collections.getOrNull



/**
 * Type alias for validators
 */
typealias HeaderValidator<T> = (T,String) -> Option<SipParseError>

/**
 * SIP parser Bean
 */
@ApplicationScoped
class SipParserService(
    private val sipUriParser: SipUriParser = SipUriParser(),
    private val nameAddrParser: NameAddrParser = NameAddrParser(sipUriParser),
    private val sipMethodParser: SipMethodParser = SipMethodParser(),
    private val fromParser: FromParser = FromParser(nameAddrParser),
    private val toParser: ToParser = ToParser(nameAddrParser),
    private val callIdParser: CallIdParser = CallIdParser(),
    private val cSeqParser: CSeqParser = CSeqParser(sipMethodParser),
    private val viaParser: ViaParser = ViaParser(),
    private val contentTypeParser: ContentTypeParser = ContentTypeParser(),
    private val contactParser: ContactParser = ContactParser(nameAddrParser)

) {

  

    companion object {

        /**
         * SIP version supported
         */
        private const val SIP_VERSION = "2.0"

        /**
         * List of allowed SIP methods
         */
        private val listAllowedMethods = listOf("INVITE", "ACK", "BYE", "CANCEL", "REGISTER", "OPTIONS", "PRACK", "UPDATE", "INFO", "SUBSCRIBE", "NOTIFY", "REFER", "MESSAGE", "PUBLISH")

        /**
         * Regular expressions to parse SIP request
         */
        private  val requestLineRegex = """^([A-Z]+)\s+([^\s]+)\s+SIP/(\d\.\d)$""".toRegex()

        /**
         * Regular expressions to parse SIP response
         */
        private val responseLineRegex = """^SIP/(\d\.\d)\s+(\d{3})\s+(.+)$""".toRegex()

        /**
         * Regular expressions to parse SIP headers
         */
        private val headerLineRegex = """^([^:]+):\s*(.+)$""".toRegex()

        /**
         * Logger
         */
        private val log = Logger.getLogger("SipParserService") 


  
        /**
         * Validator for a SIP header check min value
         * @param min the minimum value
         * @return the validator function
         */
        private fun min(min:Int) : HeaderValidator<Int> {
            return { value,headerName -> if(value < min) Some(SipParseError.InvalidFormat("Header $headerName should be higher than $min")) else None }
        }

        /**
         * Validator for a SIP header check max value
         * @param max the maximum value
         * @return the validator function
         */
        private fun max(max:Int) : HeaderValidator<Int> {
            return { value,headerName -> if(value > max) Some(SipParseError.InvalidFormat("Header $headerName should be smaller than: $max")) else None }
        }

   

        /**
         * Validator for a SIP header check if the value is in a list
         * @param list the list of allowed values
         * @return the validator function
         */
        private fun match(regex: String) : HeaderValidator<String> {
            return { value,headerName -> if(Regex(regex).containsMatchIn(value)) None else Some(SipParseError.InvalidFormat("Header $headerName should match Regex $regex")) }
        }


        /**
         * Parse a string value & do the validation
         * @param value the value to parse
         * @param headerName the name of the header
         * @param validators the list of validators to apply
         * @return the parsed value or an error if the value is invalid
         */
        private fun parseString(value: String, headerName:String, validators: Array<HeaderValidator<String>>): Either<SipParseError, String> {
            val tvalue = value.trim()
            val listErrors : List<SipParseError> = validators.mapNotNull { validator -> validator(tvalue,headerName).getOrNull()}
            if(listErrors.isNotEmpty())
                return Either.Left(SipParseError.MultipleError(listErrors))
            else
                return Either.Right(tvalue)
        }

        /**
         * Parse an integer value & do the validation
         * @param value the value to parse
         * @param headerName the name of the header
         * @param validators the list of validators to apply
         * @return the parsed value or an error if the value is invalid
         */
        private fun parseInt(value: String, headerName: String,validators: Array<HeaderValidator<Int>>): Either<SipParseError, Int> {
            try{
                val intValue = value.toInt()
                val listErrors : List<SipParseError> = validators.mapNotNull { validator -> validator(intValue,headerName).getOrNull()}
                if(listErrors.isNotEmpty())
                    return Either.Left(SipParseError.MultipleError(listErrors))
                else
                    return Either.Right(intValue)
            }
            catch(e: Exception){
                return Either.Left(SipParseError.InvalidFormat("Invalid integer value: $value"))
            }
          
        }

        /**
         * Parse a string array value & do the validation
         * @param value the value to parse
         * @param separator the separator to split the values
         * @param headerName the name of the header
         * @param validators the list of validators to apply
         * @return the parsed value or an error if the value is invalid
         */
        private fun parseStringArray(value: String, separator: String, headerName:String, validators: Array<HeaderValidator<String>>): Either<SipParseError, List<String>> {
            val values = value.split(separator)
            val listErrors : List<SipParseError> = listOf()
            
            values.forEach { 
                val res = parseString(it,headerName,validators)
                when(res){
                    is Either.Left -> listErrors.plus(res.value)
                    is Either.Right -> {}
                }
            }
            if(listErrors.isNotEmpty())
                return Either.Left(SipParseError.MultipleError(listErrors))
            else
                return Either.Right(values)
    
        }

   

    }
    
    /**
     * Parse a SIP message
     * @param message the message to parse
     * @return the parsed message or an error if the message is invalid
     */
    fun parse(message: String): Either<SipParseError, SipMessage> {
        val lines = message.lines()
        if (lines.isEmpty())
            return Either.Left(SipParseError.InvalidFormat("Empty message"))

        return try {
            val firstLine = lines[0]
            val isRequest = requestLineRegex.containsMatchIn(firstLine)
            val isResponse = responseLineRegex.containsMatchIn(firstLine)
            val (headerLines, bodyLines) = splitHeadersAndBody(lines.drop(1))
            val headers = parseHeaders(headerLines)

            if (isRequest) {
                parseRequest(firstLine, headers, bodyLines.joinToString("\n"))
            } else if (isResponse) {
                parseResponse(firstLine, headers, bodyLines.joinToString("\n"))
            } else {
                Either.Left(SipParseError.InvalidFormat("Invalid message type"))
            }
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat(e.message ?: "Unknown error"))
        }
    }

    /**
     * Parse a SIP request
     * @param firstLine the first line of the request
     * @param headers the headers of the request
     * @param body the body of the request
     * @return the parsed request or a parse error
     */
    private fun parseRequest(firstLine: String, headers: Map<String, List<String>>, body: String): Either<SipParseError, SipRequest> {
        val match = requestLineRegex.find(firstLine) ?: 
            return Either.Left(SipParseError.InvalidFormat("Invalid request line format"))
        
        val (methodS, uriS, version) = match.destructured
        
        //Initialize builder
        var builder: SipRequest.SipRequestBuilder = SipRequest.SipRequestBuilder()

        /**
         * Set the sip method, the sip version & uri
         */
        builder.sipMethod(sipMethodParser.parse(methodS))
        builder.uri(sipUriParser.parse(uriS))
        builder.sipVersion(parseSipVersion(version))

      

        //Treat the headers
        headers.forEach { (name, values) ->
            when (name) {
                "From" ,"f" -> builder.from(fromParser.parse(values[0]))                   
                "To" , "t" -> builder.to(toParser.parse(values[0]))
                "Call-ID", "i" -> builder.callId(callIdParser.parse(values[0]))
                "CSeq" -> builder.cSeq(cSeqParser.parse(values[0]))
                "Via", "v" -> values.forEach { value -> builder.via(viaParser.parse(value)) }
                "Content-Type", "c" -> builder.contentType(contentTypeParser.parse(values[0]))
                "Content-Length", "l" -> builder.contentLength(parseInt(values[0], name, arrayOf(min(0))))
                "Max-Forwards" -> builder.maxForwards(parseMaxForwards(values[0]))
                "Contact" -> values.forEach{ value -> builder.contact(contactParser.parse(value))}
                "Expires" -> values.forEach{ value -> builder.expires(parseInt(value, name, arrayOf(min(0))))}
                "Min-Expires" -> values.forEach{ value -> builder.minExpires(parseInt(value, name, arrayOf(min(0))))}
                "User-Agent" -> values.forEach{ value -> builder.userAgent(parseString(value, name, arrayOf()))}
                "Allow" -> values.forEach{ value -> builder.allow(parseStringArray(value, ",", name, arrayOf()))}
                "Server" -> values.forEach{ value -> builder.server(parseString(value, name, arrayOf()))}
                "Require" -> values.forEach{ value -> builder.require(parseStringArray(value, ",", name, arrayOf()))}
                "Supported" -> values.forEach{ value -> builder.supported(parseStringArray(value, ",", name, arrayOf()))}
                
                else -> values.forEach{ value -> builder.header(name, value)}
               
            }
            
        }

        builder.addBody(body)

        return builder.build()
        
    }

    /**
     * Parse a Sip response
     * @param firstLine the first line of the response
     * @param headers the headers of the response
     * @param body the body of the response
     * @return the parsed response or a parse error
     */
    private fun parseResponse(firstLine: String, headers: Map<String, List<String>>, body: String): Either<SipParseError, SipResponse> {
        val match = responseLineRegex.find(firstLine) ?: 
            return Either.Left(SipParseError.InvalidFormat("Invalid response line format"))
        
        val (version, statusCode, reasonPhrase) = match.destructured

        //Initialize builder
        val builder = SipResponse.SipResponseBuilder()

        //Set the sip version, status code and reason phrase
        builder.sipVersion(parseSipVersion(version))
        builder.statusCode(parseStatusCode(statusCode))
        builder.reasonPhrase(Either.Right(reasonPhrase))

        val psc = parseStatusCode(statusCode)

        //Treat the headers
        headers.forEach{ (name,values) ->
            when(name) {
                "From" ,"f" -> builder.from(fromParser.parse(values[0]))                   
                "To" , "t" -> builder.to(toParser.parse(values[0]))
                "Call-ID", "i" -> builder.callId(callIdParser.parse(values[0]))
                "CSeq" -> builder.cSeq(cSeqParser.parse(values[0]))
                "Via", "v" -> values.forEach { value -> builder.via(viaParser.parse(value)) }
                "Content-Type", "c" -> builder.contentType(contentTypeParser.parse(values[0]))
                "Content-Length", "l" -> builder.contentLength(parseContentLenght(values[0]))
                "Max-Forwards" -> builder.maxForwards(parseMaxForwards(values[0]))
                "Contact" -> values.forEach{ value -> builder.contact(contactParser.parse(value))}
                "Expires" -> values.forEach{ value -> builder.expires(parseInt(value, name, arrayOf(min(0))))}
                "Min-Expires" -> values.forEach{ value -> builder.minExpires(parseInt(value, name, arrayOf(min(0))))}
                "User-Agent" -> values.forEach{ value -> builder.userAgent(parseString(value, name, arrayOf()))}
                "Allow" -> values.forEach{ value -> builder.allow(parseStringArray(value, ",", name, arrayOf()))}
                "Server" -> values.forEach{ value -> builder.server(parseString(value, name, arrayOf()))}
                "Require" -> values.forEach{ value -> builder.require(parseStringArray(value, ",", name, arrayOf()))}
                "Supported" -> values.forEach{ value -> builder.supported(parseStringArray(value, ",", name, arrayOf()))}

                else -> values.forEach{ value -> builder.header(name, value)}
            }

        }
        builder.addBody(body)
        return builder.build()        
    }

    /**
     * Parse a status code
     */
    private fun parseStatusCode(statusCode: String): Either<SipParseError, Int> {
        return try {
            val code = statusCode.toInt()
            if(code in 100..699) 
                Either.Right(code)
            else 
                Either.Left(SipParseError.InvalidFormat("Invalid status code: $statusCode"))
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat("Invalid status code: $statusCode"))
        }
    }

        /**
     * Parse a status code
     */
    private fun parseMaxForwards(maxForwards: String): Either<SipParseError, Int> {
        return try {
            val max = maxForwards.toInt()
            if(max >= 0 ) 
                Either.Right(max)
            else 
                Either.Left(SipParseError.InvalidFormat("Invalid Max-forwards $maxForwards"))
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat("Invalid Max-forwards: $maxForwards"))
        }
    }

    /**
     * Parse a content-lenght
     */
    private fun parseContentLenght(lenght:String): Either<SipParseError, Int> {
        return try {
            val length = lenght.toInt()
            if(length >= 0) 
                Either.Right(lenght.toInt())
            else 
                Either.Left(SipParseError.InvalidFormat("Invalid content length: $lenght"))
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat("Invalid content length: $lenght"))
        }
    }

    /**
     * Parse the SIP version
     * @param version the version string to parse
     * @return the parsed version or an error if the version is invalid
     */
    private fun parseSipVersion(version: String): Either<SipParseError, SipVersion> {
        return if (version == SIP_VERSION) {
            Either.Right(SipVersion(SipVersionValue.SIP_2_0))
        } else {
            Either.Left(SipParseError.InvalidFormat("Invalid SIP version: $version"))
        }
    }

    /**
     * Parse the headers of a SIP message
     * @param headerLines the lines containing the headers
     * @return the parsed headers
     */
    private fun parseHeaders(headerLines: List<String>): Map<String, List<String>> {
        val headers = mutableMapOf<String, MutableList<String>>()
        var currentHeader: String? = null
        var currentValue = StringBuilder()

        //For each headers doe the parsing
        for (line in headerLines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                currentValue.append(" ").append(line.trim())
            } else {
                if (currentHeader != null) {
                    headers.getOrPut(currentHeader) { mutableListOf() }
                        .add(currentValue.toString().trim())
                }

                val match = headerLineRegex.find(line) ?: 
                    throw IllegalArgumentException("Invalid header format: $line")
                val (name, value) = match.destructured
                currentHeader = name.trim()
                currentValue = StringBuilder(value)
            }
        }

        if (currentHeader != null) {
            headers.getOrPut(currentHeader) { mutableListOf() }
                .add(currentValue.toString().trim())
        }

        return headers
    }

    /**
     * Split the headers and the body of a SIP message
     * @param lines the lines of the message
     * @return a pair containing the headers and the body
     */
    private fun splitHeadersAndBody(lines: List<String>): Pair<List<String>, List<String>> {
        val emptyLineIndex = lines.indexOf("")
        return if (emptyLineIndex == -1) {
            Pair(lines, emptyList())
        } else {
            Pair(lines.take(emptyLineIndex), lines.drop(emptyLineIndex + 1))
        }
    }

    /**
     *  Concatenate the headers to a string
     * @param headerName the name of the header 
     * @param headerValue the value of the header
     * @param sb the string builder
     */
    private fun headerToString(headerName : String, headerValue: String, sb: StringBuilder){
        sb.append(headerName)
        sb.append(": ")
        sb.append(headerValue)
        sb.append("\r\n")
    }


    /**
     * Convert a SIP message to a string
     * @param sipMessage the message to convert
     * @return the string representation of the message
     */
    fun toString(sipMessage: SipMessage): String {
        val sb = StringBuilder()

        when (sipMessage) {
            is SipRequest -> {
                // First line for request
                sb.append(sipMessage.method.method)
                sb.append(" ")
                sb.append(sipUriParser.toString(sipMessage.uri))
                sb.append(" SIP/")
                sb.append(SIP_VERSION)
                sb.append("\r\n")
            }
            is SipResponse -> {
                // First line for response
                sb.append("SIP/")
                sb.append(SIP_VERSION)
                sb.append(" ")
                sb.append(sipMessage.statusCode)
                sb.append(" ")
                sb.append(sipMessage.reasonPhrase)
                sb.append("\r\n")
            }
        }

        // Mandatory Headers
        sipMessage.via.forEach { via -> 
            headerToString("Via", viaParser.toString(via), sb)
        }

        headerToString("From", fromParser.toString(sipMessage.from), sb)
        headerToString("To", toParser.toString(sipMessage.to), sb)
        headerToString("Call-ID", callIdParser.toString(sipMessage.callId), sb)
        headerToString("CSeq", cSeqParser.toString(sipMessage.cSeq), sb)
        headerToString("Max-Forwards", sipMessage.maxForwards.toString(), sb)
        
        sipMessage.contact.forEach { contact ->
            headerToString("Contact", contactParser.toString(contact), sb)
        }


        // Optional Headers
        sipMessage.contentTypeHeader.map { contentType ->
            headerToString("Content-Type", contentTypeParser.toString(contentType), sb)
        }

        sipMessage.contentLength.map { length ->
            headerToString("Content-Length", length.toString(), sb)
        }

        sipMessage.expires.map { expires ->
            headerToString("Expires", expires.toString(), sb)
        }

        sipMessage.minExpires.map { minExpires ->
            headerToString("Min-Expires", minExpires.toString(), sb)
        }

        sipMessage.userAgent.map { userAgent ->
            headerToString("User-Agent", userAgent, sb)
        }

        sipMessage.allow.map { allow ->
            headerToString("Allow", allow.joinToString(", "), sb)
        }

        sipMessage.server.map { server ->
            headerToString("Server", server, sb)
        }

        sipMessage.require.map { require ->
            headerToString("Require", require.joinToString(", "), sb)
        }

        sipMessage.supported.map { supported ->
            headerToString("Supported", supported.joinToString(", "), sb)
        }

 

        // Additional headers
        sipMessage.headers.forEach { (name, values) ->
            values.forEach { value ->
                sb.append(name)
                sb.append(": ")
                sb.append(value)
                sb.append("\r\n")
            }
        }

        // Empty line separator
        sb.append("\r\n")

        // Body
        if (sipMessage.body.isNotEmpty()) {
            sb.append(sipMessage.body.joinToString("\n"))
        }

        return sb.toString()
    }

   
}
package org.cenva.parser

import arrow.core.Either

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import jakarta.enterprise.context.ApplicationScoped

import org.cenva.dao.sip.*



import org.jboss.logging.Logger;



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
                "Content-Length", "l" -> builder.contentLength(parseContentLenght(values[0]))
                "Max-Forwards" -> builder.maxForwards(parseMaxForwards(values[0]))
                "Contact" -> values.forEach{ value -> builder.contact(contactParser.parse(value))}
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

        // Headers
        sipMessage.via.forEach { via -> 
            sb.append("Via: ")
            sb.append(viaParser.toString(via))
            sb.append("\r\n")
        }

        sb.append("From: ")
        sb.append(fromParser.toString(sipMessage.from))
        sb.append("\r\n")

        sb.append("To: ")
        sb.append(toParser.toString(sipMessage.to))
        sb.append("\r\n")

        sb.append("Call-ID: ")
        sb.append(callIdParser.toString(sipMessage.callId))
        sb.append("\r\n")

        sb.append("CSeq: ")
        sb.append(cSeqParser.toString(sipMessage.cSeq))
        sb.append("\r\n")

        sipMessage.contact.forEach { contact ->
            sb.append("Contact: ")
            sb.append(contactParser.toString(contact))
            sb.append("\r\n")
        }

        sipMessage.contentTypeHeader.map { contentType ->
            sb.append("Content-Type: ")
            sb.append(contentTypeParser.toString(contentType))
            sb.append("\r\n")
        }

        sipMessage.contentLength.map { length ->
            sb.append("Content-Length: ")
            sb.append(length)
            sb.append("\r\n")
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
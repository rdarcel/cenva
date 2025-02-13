package org.cenva.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.cenva.dao.sip.*

import org.jboss.logging.Logger


/**
 * Parser for From header fields as specified in RFC 3261
 */
abstract class AdressParser<T:SipObject>(private val nameAddrParser: NameAddrParser) : ISipParserProvider<T> {
    
    companion object {
        /*
            * Logger
        */
        private val log = Logger.getLogger(AdressParser::class.java)

        /**
         * Regex for parsing address fields
         */        
        private val ADDRESS_PARSER_REGEX = """^((?:\"?[^\"<]*\"?\s*)?<[^>]+>)[;]?(.*)$""".toRegex()



        /**
         * Regex for SIP URI parameters
         */
        private val SIP_PARAM_REGEX = """([^=;]+)(?:=([^;]+))?""".toRegex()
    }

    /**
     * Parse the Adress header
     */
    protected fun parseNameAddr(message:String): Either<SipParseError, AddressHeader> {
            // Split the From header into nameaddr part and parameters part
            val match = ADDRESS_PARSER_REGEX.find(message) ?: return Either.Left(SipParseError.InvalidFormat("Invalid address format for ${message}"))

            val (nameAddr, paramsStr) = match.destructured

            // Parse the nameaddr part
            val nameAddrResult = nameAddrParser.parse(nameAddr)

            
            // Return based on the result of the nameaddr parsing
            return when(nameAddrResult) {
                is Either.Left -> Either.Left(SipParseError.InvalidFormat("Invalid name-addr format for ${nameAddr}"))
                is Either.Right -> { 

                    val parameters = mutableMapOf<String, Option<String>>()
                    var tag: Option<String> = None
    
                    // Parse parameters
                    if (paramsStr.isNotEmpty()) {
                        SIP_PARAM_REGEX.findAll(paramsStr).forEach { paramMatch ->
                            val (name, value) = paramMatch.destructured
                            parameters[name] = if (value.isNotEmpty()) Some(value) else None
                            if(name == "tag") {
                                tag = if (value.isNotEmpty()) Some(value) else None
                            }
                        }
                    }
                         
                    Either.Right(AddressHeader(nameAddrResult.value, tag, parameters))
                  
                }
            }
        }

        
    

    /**
     * Convert an Adress header object to a string
     * @param obj the Adress Header object to convert
     * 
     */
    fun toStringAdress(obj: AddressHeader): String {
        val sb = StringBuilder()
        
        // Add the name-addr part
        sb.append(nameAddrParser.toString(obj.nameAddr))
              
        // Add other parameters
        obj.parameters.forEach { (name, value) ->
            sb.append(";").append(name)
            value.map { sb.append("=").append(it) }
        }
        
        return sb.toString()
    }
}
/**
 * Parser for From header fields as specified in RFC 3261
 * @param nameAddrParser the parser for the name-addr part of the From header
 */
class FromParser(nameAddrParser: NameAddrParser) : AdressParser<FromHeader>(nameAddrParser) {

    /**
     * Parse a From header field value as defined in RFC 3261
     * @param message the From header field value to parse
     */
    override fun parse(message: String): Either<SipParseError, FromHeader> {
        return parseNameAddr(message).map { FromHeader(it) }
    }

    /**
     * Convert a From header field value to its string representation
     * @param obj the FromHeader object to convert
     */
    override fun toString(obj: FromHeader): String {
        return toStringAdress(obj.address)
    }
}


/**
 * Parser for To header fields as specified in RFC 3261
 * @param nameAddrParser the parser for the name-addr part of the To header
*/
class ToParser(nameAddrParser: NameAddrParser) : AdressParser<ToHeader>(nameAddrParser) {

    /**
     * Parse a To header field value as defined in RFC 3261
     * @param message the To header field value to parse
     */
    override fun parse(message: String): Either<SipParseError, ToHeader> {
        return parseNameAddr(message).map { ToHeader(it) }
    }

    /**
     * Convert a To header field value to its string representation
     * @param obj the ToHeader object to convert
     */
    override fun toString(obj: ToHeader): String {
        return toStringAdress(obj.address)
    }
}

/**
 * Parser for Contact header fields as specified in RFC 3261
 * @param nameAddrParser the parser for the name-addr part of the Contact header
 */
class ContactParser(nameAddrParser: NameAddrParser) : AdressParser<ContactHeader>(nameAddrParser) {

    /**
     * Parse a Contact header field value as defined in RFC 3261
     */
    override fun parse(message: String): Either<SipParseError, ContactHeader> {
        return parseNameAddr(message).map { ContactHeader(it) }
    }

    /**
     * Convert a Contact header field value to its string representation
     */
    override fun toString(obj: ContactHeader): String {
        return toStringAdress(obj.address)
    }
}
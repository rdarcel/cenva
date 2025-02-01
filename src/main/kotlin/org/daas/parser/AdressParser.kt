package org.daas.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.daas.dao.sip.SipParseError
import org.daas.dao.sip.SipObject
import org.daas.dao.sip.NameAddr
import org.daas.dao.sip.FromHeader
import org.daas.dao.sip.AddressHeader
import org.daas.dao.sip.SipUri

import org.jboss.logging.Logger


/**
 * Parser for From header fields as specified in RFC 3261
 */
abstract class AdressParser(private val nameAddrParser: NameAddrParser) : ISipParserProvider<FromHeader> {
    
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
     * Parse a an adress field
     * @param message the Adress header field to parse
     * @return Either an error or a FromHeader object
     */
    override fun parse(message: String): Either<SipParseError, FromHeader> {
    
            // Split the From header into nameaddr part and parameters part
            val match = ADDRESS_PARSER_REGEX.find(message) 
            ?: return Either.Left(SipParseError.InvalidFormat("Invalid address format for ${message}"))

            val (nameAddr, paramsStr) = match.destructured
            // Parse the nameaddr part
            val nameAddrResult = nameAddrParser.parse(nameAddr)

            
            // Return based on the result of the nameaddr parsing
            return when(nameAddrResult) {
                is Either.Left -> Either.Left(SipParseError.InvalidFormat("Invalid name-addr format for ${nameAddr}"))
                is Either.Right -> { 
                    log.info("###################Parsed value is "+nameAddrResult.value)

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
                    
           
                               
                    return Either.Right(FromHeader(AddressHeader(nameAddrResult.value, tag, parameters)))
                }
            }
        }

        
    

    /**
     * Convert a FromHeader object to a string
     * @param obj the FromHeader object to convert
     * 
     */
    override fun toString(obj: FromHeader): String {
        val sb = StringBuilder()
        
        // Add the name-addr part
        sb.append(nameAddrParser.toString(obj.address.nameAddr))
              
        // Add other parameters
        obj.address.parameters.forEach { (name, value) ->
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
class FromParser(nameAddrParser: NameAddrParser) : AdressParser(nameAddrParser) {
    override fun fieldName(): String = "From"
}


/**
 * Parser for To header fields as specified in RFC 3261
 * @param nameAddrParser the parser for the name-addr part of the To header
*/
class ToParser(nameAddrParser: NameAddrParser) : AdressParser(nameAddrParser) {
    override fun fieldName(): String = "To"
}

/**
 * Parser for Contact header fields as specified in RFC 3261
 * @param nameAddrParser the parser for the name-addr part of the Contact header
 */
class ContactParser(nameAddrParser: NameAddrParser) : AdressParser(nameAddrParser) {
    override fun fieldName(): String = "Contact"
}
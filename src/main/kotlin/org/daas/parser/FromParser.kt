package org.daas.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.daas.dao.SipParseError
import org.daas.dao.SipObject
import org.daas.dao.NameAddr
import org.daas.dao.FromHeader
import org.daas.dao.SipUri

import org.jboss.logging.Logger




/**
 * Parser for From header fields as specified in RFC 3261
 */
class FromParser(private val nameAddrParser: NameAddrParser) : ISipParserProvider<FromHeader> {
    
    companion object {
        private val log = Logger.getLogger(FromParser::class.java)
        private val PARAM_REGEX = """([^=;]+)(?:=([^;]+))?""".toRegex()
    }

    /**
     * Name of the field
     */
    override fun fieldName(): String = "From"

    override fun parse(message: String): Either<SipParseError, FromHeader> {
        try {
            // Split the From header into nameaddr part and parameters part
            val parts = message.split(";")
            
            // Parse the nameaddr part
            val nameAddrResult = nameAddrParser.parse(parts[0])
            
            return nameAddrResult.map { nameAddr ->
                // Parse parameters if they exist
                val parameters = mutableMapOf<String, Option<String>>()
                var tag: Option<String> = None
                
                // Process remaining parts as parameters
                if (parts.size > 1) {
                    parts.drop(1).forEach { param ->
                        val match = PARAM_REGEX.find(param.trim()) ?: 
                            return Either.Left(SipParseError.InvalidFormat("Invalid parameter format: $param"))
                        
                        val (name, value) = match.destructured
                        when (name) {
                            "tag" -> tag = if (value.isNotEmpty()) Some(value) else None
                            else -> parameters[name] = if (value.isNotEmpty()) Some(value) else None
                        }
                    }
                }
                
                FromHeader(nameAddr, tag, parameters)
            }
        } catch (e: Exception) {
            log.error("Error parsing From header: ${e.message}")
            return Either.Left(SipParseError.InvalidFormat("Failed to parse From header: ${e.message}"))
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
        sb.append(nameAddrParser.toString(obj.nameAddr))
        
        // Add tag parameter if present
        obj.tag.map { sb.append(";tag=").append(it) }
        
        // Add other parameters
        obj.parameters.forEach { (name, value) ->
            sb.append(";").append(name)
            value.map { sb.append("=").append(it) }
        }
        
        return sb.toString()
    }
}
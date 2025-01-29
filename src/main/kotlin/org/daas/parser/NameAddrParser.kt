package org.daas.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.daas.dao.NameAddr
import org.daas.dao.SipParseError
import org.daas.dao.SipUri

/**
 * Parser for name-addr header fields according to RFC 3261
 */
class NameAddrParser(private val sipUriParser: SipUriParser) : ISipParserProvider<NameAddr> {

    /**
     * Regex for parsing name-addr fields
     */
    companion object {
        private val NAME_ADDR_REGEX = """^(?:"([^"]*)")?[ ]*(?:<(.+)>|([^;> ]+))(?:[ ]*;(.*))?$""".toRegex()
        private val PARAM_REGEX = """([^=;]+)(?:=([^;]+))?""".toRegex()
    }

    /**
     * Define the name of the field
     */
    override fun fieldName(): String = "name-addr"

    /**
     * Parse a name-addr field
     * @param message the name-addr field to parse
     */
    override fun parse(message: String): Either<SipParseError, NameAddr> {
        return try {
            val match = NAME_ADDR_REGEX.find(message.trim()) 
                ?: return Either.Left(SipParseError.InvalidFormat("Invalid name-addr format"))

            val (displayName, bracketedUri, unbracketedUri, params) = match.destructured

            // Parse the URI part
            val uriString = bracketedUri.ifEmpty { unbracketedUri }
            val uriResult = sipUriParser.parse(uriString)

            when (uriResult) {
                is Either.Left -> return Either.Left(SipParseError.InvalidUri("Invalid URI in name-addr: ${uriResult.value.message}"))
                is Either.Right -> {
                    // Parse parameters if present
                    val parameters = mutableMapOf<String, Option<String>>()
                    if (params.isNotEmpty()) {
                        PARAM_REGEX.findAll(params).forEach { paramMatch ->
                            val (name, value) = paramMatch.destructured
                            parameters[name] = if (value.isNotEmpty()) Some(value) else None
                        }
                    }

                    Either.Right(NameAddr(
                        displayName = if (displayName.isNotEmpty()) Some(displayName) else None,
                        uri = uriResult.value,
                        parameters = parameters
                    ))
                }
            }
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat("Failed to parse name-addr: ${e.message}"))
        }
    }

    /**
     * Convert a name-addr object to a string
     * @param obj the name-addr object to convert
     * @return the string representation of the name-addr object in SIP
     */
    override fun toString(obj: NameAddr): String {
        val sb = StringBuilder()
        
        // Add display name if present
        obj.displayName.map { displayName ->
            sb.append("\"").append(displayName).append("\" ")
        }

        // Add URI
        sb.append("<").append(sipUriParser.toString(obj.uri)).append(">")

        // Add parameters
        obj.parameters.forEach { (key, value) ->
            sb.append(";").append(key)
            value.map { sb.append("=").append(it) }
        }

        return sb.toString()
    }
}



package org.cenva.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.cenva.dao.sip.NameAddr
import org.cenva.dao.sip.SipParseError
import org.cenva.dao.sip.SipUri

/**
 * Parser for name-addr header fields according to RFC 3261
 */
class NameAddrParser(private val sipUriParser: SipUriParser) : ISipParserProvider<NameAddr> {

    /**
     * Regex for parsing name-addr fields
     */
    companion object {
        /**
         * Regex for parsing name-addr fields with display name, URI and parameters.
         * Group 1: Display name (optionals, with double quote)
         * Group 2 : Display name without quote (optional)
         * Group 2: URI 

         */
        private val NAME_ADDR_REGEX = """^(?:(?<!\\)"((?:\\.|[^"\\])*)?(?<!\\)"|([^ <]*))[ ]*<([^>]*)>$""".toRegex()




    }


    /**
     * Parse a name-addr field
     * @param message the name-addr field to parse
     */
    override fun parse(message: String): Either<SipParseError, NameAddr> {
        return try {
            val match = NAME_ADDR_REGEX.find(message) 
                ?: return Either.Left(SipParseError.InvalidFormat("Invalid name-addr format for ${message}"))

            val (displayNameWithQuote, displayNameWithoutQuote, uri) = match.destructured

            var displayName = if(displayNameWithQuote.isNotEmpty()) displayNameWithQuote else displayNameWithoutQuote
            displayName = displayName.replace("\\\"", "\"")

            // Parse the URI part
            val uriResult = sipUriParser.parse(uri)

            when (uriResult) {
                is Either.Left -> return Either.Left(SipParseError.InvalidUri("Invalid URI in name-addr: ${uriResult.value.message}"))
                is Either.Right -> {
         

                    Either.Right(NameAddr(
                        displayName = if (displayName.isNotEmpty()) Some(displayName.trim()) else None,
                        uri = uriResult.value
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

   

        return sb.toString()
    }
}



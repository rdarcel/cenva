package org.cenva.parser

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import arrow.core.None
import org.cenva.dao.sip.SipParseError
import org.cenva.dao.sip.SipObject
import org.cenva.dao.sip.CallIdHeader


/**
 * Parser for Call-ID header fields as specified in RFC 3261
 */
class CallIdParser : ISipParserProvider<CallIdHeader> {
    companion object {
        private val CALLID_REGEX = """^([^@]+)(?:@(.+))?$""".toRegex()
    }


    /**
     * Parse a Call-ID header field value
     * @param message the Call-ID header field value to parse
     * @return Either an error or a CallIdHeader object
     */
    override fun parse(message: String): Either<SipParseError, CallIdHeader> {
        if (message.isEmpty()) {
            return Either.Left(SipParseError.EmptyMessage("Call-ID header field is empty"))
        }

        val match = CALLID_REGEX.find(message)
            ?: return Either.Left(SipParseError.InvalidFormat("Invalid Call-ID format: $message"))

        val (identifier, host) = match.destructured

        return Either.Right(
            CallIdHeader(
                identifier = identifier,
                host = if (host.isNotEmpty()) Some(host) else None
            )
        )
    }

    /**
     * Convert a CallIdHeader object to a string
     * @param obj the CallIdHeader object to convert
     * @return the string representation of the CallIdHeader
     */
    override fun toString(obj: CallIdHeader): String {
        return buildString {
            append(obj.identifier)
            obj.host.map { append("@").append(it) }
        }
    }
}
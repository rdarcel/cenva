package org.daas.parser

import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import arrow.core.None
import org.daas.dao.sip.SipParseError
import org.daas.dao.sip.SipObject
import org.daas.dao.sip.CallId


/**
 * Parser for Call-ID header fields as specified in RFC 3261
 */
class CallIdParser : ISipParserProvider<CallId> {
    companion object {
        private val CALLID_REGEX = """^([^@]+)(?:@(.+))?$""".toRegex()
    }

    override fun fieldName(): String = "Call-ID"

    /**
     * Parse a Call-ID header field value
     * @param message the Call-ID header field value to parse
     * @return Either an error or a CallIdHeader object
     */
    override fun parse(message: String): Either<SipParseError, CallId> {
        if (message.isEmpty()) {
            return Either.Left(SipParseError.EmptyMessage)
        }

        val match = CALLID_REGEX.find(message)
            ?: return Either.Left(SipParseError.InvalidFormat("Invalid Call-ID format: $message"))

        val (identifier, host) = match.destructured

        return Either.Right(
            CallId(
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
    override fun toString(obj: CallId): String {
        return buildString {
            append(obj.identifier)
            obj.host.map { append("@").append(it) }
        }
    }
}
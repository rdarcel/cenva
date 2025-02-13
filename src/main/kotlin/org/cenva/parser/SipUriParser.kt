package org.cenva.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.cenva.dao.sip.SipUri
import org.cenva.dao.sip.SipParseError

import org.jboss.logging.Logger;

/**
 * Procvides a parser for SIP URIs
 * @param allowMail if true, allows parsing of mailto URIs
 */
class SipUriParser(val allowMail: Boolean = false) : ISipParserProvider<SipUri> {

    /**
     * Reference the regex to be used to parse
     */
    companion object {
      /**
         * Regex for TEL URI as defined in RFC 3966.
         *
         * Breakdown:
         *  - (^tel:)         : Ensures the string starts with "tel:".
         *  - ([+\dA-Za-z\-\.\*#]+)
         *                     : Captures the telephone-subscriber part which may be a global number (starting with '+')
         *                       or a local number containing digits and allowed punctuation.
         *  - (?:;([^;=]+(?:=[^;]+)?(?:;[^;=]+(?:=[^;]+)?)*))?
         *                     : Optionally captures parameters without including the first ';'. Each parameter
         *                       matches the pattern where its name and optional value do not include ';' or '='.
         *  - ($)             : End of string.
         */
        private val TEL_URI_REGEX = """^tel:([+\dA-Za-z\-\.\*#]+)(?:;([^;=]+(?:=[^;]+)?(?:;[^;=]+(?:=[^;]+)?)*))?$""".toRegex()

        /** 
         * Regex for TEL URI parameters
         */
        private val TEL_PARAM_REGEX = """;([^=]+)(?:=(.+))?""".toRegex()

        /**
         * Regex for SIP URI
         */
        private val SIP_URI_REGEX = """^(sips?):(?:([a-zA-Z0-9\-\_\.\!\~\*\'\(\)&=\+\$,;\?\/\%]+)(:[a-zA-Z0-9\-\_\.\!\~\*\'\(\)&=\+\$,;\?\/\%]+)?@)?([[a-zA-Z0-9\-\_\.]]+)(?::(\d+))?(?:;([^?]*))?(?:\?(.+))?$""".toRegex()

        /**
         * Regex for MAILTO URI
         */
        private val MAILTO_URI_REGEX = """^mailto:([a-zA-Z0-9\-\_\.\!\~\*\'\(\)&=\+\$,;\?\/\%]+)@([[a-zA-Z0-9\-\_\.]]+)\??(?:([^?]*))$""".toRegex()

  

        /**
         * Regex for SIP URI parameters
         */
        private val SIP_PARAM_REGEX = """([^=;]+)(?:=([^;]+))?""".toRegex()

        /**
         * Regex for SIP URI headers
         */
        private val HEADER_REGEX = """([^=&]+)=([^&]+)""".toRegex()

        /**
         * Logger
         */
        private val log = Logger.getLogger(SipUriParser::class.java)
    }

    /**
     * Parse a SIP URI
     * @param message the sip uri to parse
     * @return the parsed SIP URI
     */
    override fun parse(message: String): Either<SipParseError, SipUri> {
        return try {
            when {
                message.startsWith("tel:") -> parseTelUri(message)
                message.startsWith("sip:") || message.startsWith("sips:") -> parseSipUri(message)
                message.startsWith("mailto:") && allowMail -> parseMailtoUri(message)
                else -> Either.Left(SipParseError.InvalidFormat("Invalid URI scheme"))
            }
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat(e.message ?: "Unknown parsing error"))
        }
    }

    /**
     * Parse a MAILTO URI
     * @param uri the MAILTO URI to parse
     * @return the parsed SIP URI or an error if the URI is invalid
     */
    private fun parseMailtoUri(uri: String): Either<SipParseError, SipUri> {
        val match = MAILTO_URI_REGEX.find(uri) ?: return Either.Left(SipParseError.InvalidFormat("Invalid MAILTO URI format"))
        val (userStr, hostStr, headersStr) = match.destructured

        // For a mail user & host must not be empty
        if(userStr.isEmpty()) return Either.Left(SipParseError.InvalidFormat("Invalid MAILTO URI format: user is required"))
        if(hostStr.isEmpty()) return Either.Left(SipParseError.InvalidFormat("Invalid MAILTO URI format: host is required"))

        val headers = mutableMapOf<String, String>()
        if (headersStr.isNotEmpty()) {
            HEADER_REGEX.findAll(headersStr).forEach { headerMatch ->
                val (name, value) = headerMatch.destructured
                headers[name] = value
            }
        }

        return Either.Right(SipUri(
            scheme = "mailto",
            userInfo = Some(userStr.trim()),
            password = None,
            host = Some(hostStr.trim()),
            port = None,
            uriParameters = mapOf(),
            headers = headers
        ))
    }

    /**
     * Parse a TEL URI
     * @param uri the TEL URI to parse
     * @return the parsed SIP URI
     */
    private fun parseTelUri(uri: String): Either<SipParseError, SipUri> {
        val match = TEL_URI_REGEX.find(uri) ?: return Either.Left(SipParseError.InvalidFormat("Invalid TEL URI format"))
        val (number, paramsString) = match.destructured
        
        val parameters = mutableMapOf<String, Option<String>>()


        TEL_PARAM_REGEX.findAll(paramsString).forEach { paramMatch ->
            val (name, value) = paramMatch.destructured
            parameters[name] = if (value.isNotEmpty()) Some(value) else None
            
        }

        return Either.Right(SipUri(
            scheme = "tel",
            userInfo = Some(number.trim()),
            password = None,
            host = None,
            port = None,
            uriParameters = parameters,
            headers = mapOf()
        ))
    }

    /**
     * Parse a SIP URI
     * @param uri the SIP URI to parse
     * @return the parsed SIP URI
     */
    private fun parseSipUri(uri: String): Either<SipParseError, SipUri> {
        val match = SIP_URI_REGEX.find(uri) ?: return Either.Left(SipParseError.InvalidFormat("Invalid SIP URI format"))
        val (scheme, user, passwordStr, hostStr, portStr, paramsStr, headersStr) = match.destructured

        val userInfo = if (user.isNotEmpty()) Some(user.trim()) else None

        //Not very nice to remove the : needs to be changed later
        val password = if (passwordStr.isNotEmpty()) Some(passwordStr.drop(1).trim()) else None

        val host = if(hostStr.isNotEmpty()) Some(hostStr.trim()) else None
        if(host.isNone()) return Either.Left(SipParseError.InvalidFormat("Invalid SIP URI format: host is required"))
        val port = if (portStr.isNotEmpty()) Some(portStr.toInt()) else None


        // Parse parameters
        val parameters = mutableMapOf<String, Option<String>>()
        if (paramsStr.isNotEmpty()) {
            SIP_PARAM_REGEX.findAll(paramsStr).forEach { paramMatch ->
                val (name, value) = paramMatch.destructured
                parameters[name] = if (value.isNotEmpty()) Some(value) else None
            }
        }

        // Parse headers
        val headers = mutableMapOf<String, String>()
        if (headersStr.isNotEmpty()) {
            HEADER_REGEX.findAll(headersStr).forEach { headerMatch ->
                val (name, value) = headerMatch.destructured
                headers[name] = value
            }
        }

        return Either.Right(SipUri(
            scheme = scheme,
            userInfo = userInfo,
            password = password,
            host = host,
            port = port,
            uriParameters = parameters,
            headers = headers
        ))
    }

    /**
     * Convert a SipUri object to a string
     * @param obj the SipUri object
     * @return the string representation of the SipUri/Tel Uri in SIP protocol
     */
    override fun toString(obj: SipUri): String {
        return when (obj.scheme) {
            "tel" -> buildTelUri(obj)
            "sip", "sips" -> buildSipUri(obj)
            else -> throw IllegalArgumentException("Invalid URI scheme: ${obj.scheme}")
        }
    }

    /**
     * Build a tel URI from a SipUri object
     * @param uri the SipUri object
     * @return the string representation of the SipUri in SIP protocol
     */
    private fun buildTelUri(uri: SipUri): String {
        val sb = StringBuilder("tel:")
        
        // Add phone number
        uri.userInfo.map { sb.append(it) }
        
        // Add other parameters
        uri.uriParameters.forEach { (key, value) ->
            sb.append(";").append(key)
            value.map { sb.append("=").append(it) }
        }

        return sb.toString()
    }

    /**
     * Build a SIP URI from a SipUri object
     * @param uri the SipUri object
     * @return the string representation of the SipUri in SIP protocol
     */
    private fun buildSipUri(uri: SipUri): String {
        val sb = StringBuilder(uri.scheme).append(":")
        
        // Add user info if present
        uri.userInfo.map { sb.append(it).append("@") }
        
        // Add host
        uri.host.map { sb.append(it) }
        
        // Add port if present
        uri.port.map { sb.append(":").append(it) }
        
        // Add parameters
        uri.uriParameters.forEach { (key, value) ->
            sb.append(";").append(key)
            value.map { sb.append("=").append(it) }
        }
        
        // Add headers
        if (uri.headers.isNotEmpty()) {
            sb.append("?")
            uri.headers.entries.joinTo(sb, "&") { "${it.key}=${it.value}" }
        }

        return sb.toString()
    }
}
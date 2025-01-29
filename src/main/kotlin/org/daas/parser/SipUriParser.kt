package org.daas.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.daas.dao.SipUri
import org.daas.dao.SipParseError

import org.jboss.logging.Logger;

/**
 * Procvides a parser for SIP URIs
 */
class SipUriParser : ISipParserProvider<SipUri> {
    /**
     * Name of the filter
     */
    override fun fieldName(): String = "uri"

    /**
     * Reference the regex to be used to parse
     */
    companion object {
        /**
         * Regex for TEL URI
         */
        private val TEL_URI_REGEX = """^tel:([^;]+)(.*)$""".toRegex()

        /** 
         * Regex for TEL URI parameters
         */
        private val TEL_PARAM_REGEX = """;([^=]+)(?:=(.+))?""".toRegex()

        /**
         * Regex for SIP URI
         */
        private val SIP_URI_REGEX = """^^(sips?):(?:([a-zA-Z0-9\-\_\.\!\~\*\'\(\)&=\+\$,;\?\/\%]+)(:[a-zA-Z0-9\-\_\.\!\~\*\'\(\)&=\+\$,;\?\/\%]+)?@)?([[a-zA-Z0-9\-\_\.]]+)(?::(\d+))?(?:;([^?]*))?(?:\?(.+))?$""".toRegex()
        //"""^(sips?):(?:([^@]+)@)?([^:;?]+)(?::(\d+))?(?:;([^?]*))?(?:\?(.+))?$"""
        //^(sips?):(?:([a-zA-Z0-9\-\_\.\!\~\*\'\(\)&=\+\$,;\?\/\%:]+)@)?([^:;?]+)(?::(\d+))?(?:;([^?]*))?(?:\?(.+))?$

        /**
         * Regex for SIP URI parameters
         */
        private val SIP_PARAM_REGEX = """([^=;]+)(?:=([^;]+))?""".toRegex()

        /**
         * Regex for SIP URI headers
         */
        private val HEADER_REGEX = """([^=&]+)=([^&]+)""".toRegex()

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
                else -> Either.Left(SipParseError.InvalidFormat("Invalid URI scheme"))
            }
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat(e.message ?: "Unknown parsing error"))
        }
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
        var phoneContext = Option.fromNullable(null as String?)
        var postDial = Option.fromNullable(null as String?)
        var isdnSubaddress = Option.fromNullable(null as String?)

        TEL_PARAM_REGEX.findAll(paramsString).forEach { paramMatch ->
            val (name, value) = paramMatch.destructured
            when (name) {
                "phone-context" -> phoneContext = Some(value)
                "postd" -> postDial = Some(value)
                "isub" -> isdnSubaddress = Some(value)
                else -> parameters[name] = if (value.isNotEmpty()) Some(value) else None
            }
        }

        return Either.Right(SipUri(
            scheme = "tel",
            userInfo = Some(number),
            password = None,
            host = None,
            port = None,
            uriParameters = parameters,
            headers = mapOf(),
            phoneContext = phoneContext,
            postDial = postDial,
            isdnSubaddress = isdnSubaddress
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

        val userInfo = if (user.isNotEmpty()) Some(user) else None
        //if(userInfo.isNone()) return Either.Left(SipParseError.InvalidFormat("Invalid SIP URI format: user is required"))

        //Not very nice to remove the : needs to be changed later
        val password = if (passwordStr.isNotEmpty()) Some(passwordStr.drop(1)) else None

        val host = if(hostStr.isNotEmpty()) Some(hostStr) else None
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
            headers = headers,
            phoneContext = None,
            postDial = None,
            isdnSubaddress = None
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
        
        // Add parameters
        uri.phoneContext.map { sb.append(";phone-context=").append(it) }
        uri.postDial.map { sb.append(";postd=").append(it) }
        uri.isdnSubaddress.map { sb.append(";isub=").append(it) }
        
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
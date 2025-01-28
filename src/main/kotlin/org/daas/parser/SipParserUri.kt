package org.daas.parser

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.none
import arrow.core.Some

import org.daas.dao.SipParseError
import org.daas.dao.SipUri

class SipParserUri : ISipParserProvider<SipUri> {
    override fun parse(message: String): Either<SipParseError, SipUri> { 
        try {
            // Basic validation
            if (message.isEmpty()) {
                return SipParseError.InvalidUri("Empty URI").left()
            }

            // Regex pattern for SIP URI
            val uriPattern = """^([\w-]+):(?:([^@]+)@)?([^:;?]+)(?::(\d+))?(?:;([^?]+))?(?:\?(.+))?$""".toRegex()
            
            val match = uriPattern.find(message) ?: return SipParseError.InvalidUri("Invalid URI format").left()
            
            // Extract components
            val scheme = match.groupValues[1]
            val userInfo = match.groupValues[2].takeIf { it.isNotEmpty() }
            val host = match.groupValues[3]
            val port = match.groupValues[4].takeIf { it.isNotEmpty() }?.toIntOrNull()
                ?: null
            
            // Parse URI parameters
            val uriParams = mutableMapOf<String, Option<String>()
            match.groupValues[5].takeIf { it.isNotEmpty() }?.let { params ->
                params.split(';').forEach { param ->
                    val parts = param.split('=', limit = 2)
                    val part = parts.getOrNull(1)
                    uriParams[parts[0]] = if(part.isNullOrEmpty()) none() else Some(part)
                }
            }

            // Parse headers
            val headers = mutableMapOf<String, String>()
            match.groupValues[6].takeIf { it.isNotEmpty() }?.let { headerStr ->
                headerStr.split('&').forEach { param ->
                    val parts = param.split('=', limit = 2)
                    if (parts.size == 2) {
                        headers[parts[0]] = parts[1]
                    }
                }
            }

            return SipUri(
                scheme = scheme,
                userInfo = if(userInfo == null) none() else Some(userInfo),
                host = host,
                port = if (port == null) none() else Some(port),
                uriParams = uriParams,
                headers = headers
            ).right()

        } catch (e: Exception) {
            return SipParseError.InvalidUri("Failed to parse URI: ${e.message}").left()
        }
    }

    /**
     * Name of the object the parser is handling
     */
    override fun fieldName(): String = "URI"



    /**
     * Convert a SipUri object to a string
     */
    override fun toString(obj: SipUri): String { 
        val builder = StringBuilder()
        builder.append(obj.scheme).append(":")

        obj.userInfo?.let { builder.append(it).append("@") }
        builder.append(obj.host)
        obj.port?.let { builder.append(":").append(it) }

        if (obj.uriParams.isNotEmpty()) {
            obj.uriParams.forEach { (key, value) ->
                builder.append(";").append(key)
                value?.let { builder.append("=").append(it) }
            }
        }

        if (obj.headers.isNotEmpty()) {
            builder.append("?")
            builder.append(obj.headers.entries.joinToString("&") { "${it.key}=${it.value}" })
        }

        return builder.toString()
    }
}
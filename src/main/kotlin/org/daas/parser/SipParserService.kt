package org.daas.parser

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import jakarta.enterprise.context.ApplicationScoped

import org.daas.dao.sip.SipMessage
import org.daas.dao.sip.SipRequest
import org.daas.dao.sip.SipResponse

import org.daas.dao.sip.SipParseError


import org.jboss.logging.Logger;

/**
 * SIP parser Bean
 */
@ApplicationScoped
class SipParserService {

    companion object {

        /**
         * SIP version supported
         */
        private const val SIP_VERSION = "2.0"

        /**
         * List of allowed SIP methods
         */
        private val listAllowedMethods = listOf("INVITE", "ACK", "BYE", "CANCEL", "REGISTER", "OPTIONS", "PRACK", "UPDATE", "INFO", "SUBSCRIBE", "NOTIFY", "REFER", "MESSAGE", "PUBLISH")

        /**
         * Regular expressions to parse SIP request
         */
        private  val requestLineRegex = """^([A-Z]+)\s+([^\s]+)\s+SIP/(\d\.\d)$""".toRegex()

        /**
         * Regular expressions to parse SIP response
         */
        private val responseLineRegex = """^SIP/(\d\.\d)\s+(\d{3})\s+(.+)$""".toRegex()

        /**
         * Regular expressions to parse SIP headers
         */
        private val headerLineRegex = """^([^:]+):\s*(.+)$""".toRegex()

        private val log = Logger.getLogger("SipParserService") 

    }
    
    /**
     * Parse a SIP message
     * @param message the message to parse
     * @return the parsed message or an error if the message is invalid
     */
    fun parse(message: String): Either<SipParseError, SipMessage> {
        val lines = message.lines()
        if (lines.isEmpty()) return SipParseError.EmptyMessage.left()

        return try {
            val firstLine = lines[0]
            val isRequest = requestLineRegex.containsMatchIn(firstLine)
            val isResponse = responseLineRegex.containsMatchIn(firstLine)
            val (headerLines, bodyLines) = splitHeadersAndBody(lines.drop(1))
            val headers = parseHeaders(headerLines)

            if (isRequest) {
                parseRequest(firstLine, headers, bodyLines.joinToString("\n"))
            } else if (isResponse) {
                parseResponse(firstLine, headers, bodyLines.joinToString("\n"))
            } else {
                SipParseError.InvalidFormat("Invalid message type").left()
            }
        } catch (e: Exception) {
            SipParseError.InvalidFormat(e.message ?: "Unknown error").left()
        }
    }

    /**
     * Parse a SIP request
     * @param firstLine the first line of the request
     * @param headers the headers of the request
     * @param body the body of the request
     * @return the parsed request or a parse error
     */
    private fun parseRequest(firstLine: String, headers: Map<String, List<String>>, body: String): Either<SipParseError, SipRequest> {
        val match = requestLineRegex.find(firstLine) ?: 
            return SipParseError.InvalidFormat("Invalid request line format").left()
        
        val (method, uri, version) = match.destructured
        //Check if the method and version are valid
        if(version !=  SIP_VERSION) {
            return SipParseError.InvalidFormat("Invalid SIP version : ${version}").left()
        }
        if(method !in listAllowedMethods) {
            return SipParseError.InvalidFormat("Invalid SIP method : ${method}").left()
        }
        return SipRequest(
            method = method,
            requestUri = uri,
            sipVersion = version,
            headers = headers,
            sdpContent = if (body.isNotEmpty()) body else null
        ).right()
    }

    /**
     * Parse a Sip response
     * @param firstLine the first line of the response
     * @param headers the headers of the response
     * @param body the body of the response
     * @return the parsed response or a parse error
     */
    private fun parseResponse(firstLine: String, headers: Map<String, List<String>>, body: String): Either<SipParseError, SipResponse> {
        val match = responseLineRegex.find(firstLine) ?: 
            return SipParseError.InvalidFormat("Invalid response line format").left()
        
        val (version, statusCode, reason) = match.destructured
        if(version !=  SIP_VERSION) {
            return SipParseError.InvalidFormat("Invalid SIP version").left()
        }
        val statusCodeInt = statusCode.toIntOrNull()
        if(statusCodeInt == null || statusCodeInt !in 100..699) {
            return SipParseError.InvalidFormat("Invalid status code").left()
        }
        return SipResponse(
            sipVersion = version,
            statusCode = statusCodeInt,
            reasonPhrase = reason,
            headers = headers,
            sdpContent = if (body.isNotEmpty()) body else null
        ).right()
    }

    /**
     * Parse the headers of a SIP message
     * @param headerLines the lines containing the headers
     * @return the parsed headers
     */
    private fun parseHeaders(headerLines: List<String>): Map<String, List<String>> {
        val headers = mutableMapOf<String, MutableList<String>>()
        var currentHeader: String? = null
        var currentValue = StringBuilder()

        //For each headers doe the parsing
        for (line in headerLines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                currentValue.append(" ").append(line.trim())
            } else {
                if (currentHeader != null) {
                    headers.getOrPut(currentHeader) { mutableListOf() }
                        .add(currentValue.toString().trim())
                }

                val match = headerLineRegex.find(line) ?: 
                    throw IllegalArgumentException("Invalid header format: $line")
                val (name, value) = match.destructured
                currentHeader = name.trim()
                currentValue = StringBuilder(value)
            }
        }

        if (currentHeader != null) {
            headers.getOrPut(currentHeader) { mutableListOf() }
                .add(currentValue.toString().trim())
        }

        return headers
    }

    /**
     * Split the headers and the body of a SIP message
     * @param lines the lines of the message
     * @return a pair containing the headers and the body
     */
    private fun splitHeadersAndBody(lines: List<String>): Pair<List<String>, List<String>> {
        val emptyLineIndex = lines.indexOf("")
        return if (emptyLineIndex == -1) {
            Pair(lines, emptyList())
        } else {
            Pair(lines.take(emptyLineIndex), lines.drop(emptyLineIndex + 1))
        }
    }

    /**
     * Convert a SIP message to a string
     * @param sipMessage the message to convert
     * @return the string representation of the message
     */
    fun toString(sipMessage: SipMessage): String = when (sipMessage) {
        is SipRequest -> buildString {
            appendLine("${sipMessage.method} ${sipMessage.requestUri} SIP/${sipMessage.sipVersion}")
            appendHeaders(sipMessage.headers)
            appendSdpContent(sipMessage.sdpContent)
        }
        is SipResponse -> buildString {
            appendLine("SIP/${sipMessage.sipVersion} ${sipMessage.statusCode} ${sipMessage.reasonPhrase}")
            appendHeaders(sipMessage.headers)
            appendSdpContent(sipMessage.sdpContent)
        }
    }

    /**
     * Append the headers to a string builder
     * @param headers the headers to append
     */
    private fun StringBuilder.appendHeaders(headers: Map<String, List<String>>) {
        headers.forEach { (name, values) ->
            values.forEach { value ->
                appendLine("$name: $value")
            }
        }
        appendLine()
    }

    /**
     * Append the SDP content to a string builder
     * @param content the content to append
     */
    private fun StringBuilder.appendSdpContent(content: String?) {
        if (content != null) {
            appendLine(content)
        }
    }
}
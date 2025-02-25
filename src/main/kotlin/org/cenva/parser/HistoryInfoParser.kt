package org.cenva.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.cenva.dao.sip.*

import org.jboss.logging.Logger


/**
 * Parser for SIP History-Info headers as defined in RFC 7044
 */
class HistoryInfoParser(
    private val sipUriParser: SipUriParser = SipUriParser()
) : ISipParserProvider<HistoryInfoHeader> {

    companion object {
        private val HISTORY_ENTRY_REGEX = """<([^>]+)>(?:;([^,]*))?""".toRegex()
        private val PARAM_REGEX = """([^=;]+)(?:=([^;]+))?""".toRegex()
        private val log = Logger.getLogger(HistoryInfoParser::class.java)
    }

    override fun parse(message: String): Either<SipParseError, HistoryInfoHeader> {
        return try {
            val entries = mutableListOf<HistoryInfoEntry>()
            
            // Split multiple entries separated by commas
            val entryStrings = message.split(",")
            
            for (entryString in entryStrings) {
                val trimmed = entryString.trim()
                val match = HISTORY_ENTRY_REGEX.find(trimmed) ?: 
                    return Either.Left(SipParseError.InvalidFormat("Invalid History-Info entry format: $trimmed"))
                
                val (uriStr, paramsStr) = match.destructured
                
                // Parse the URI
                val uriResult = sipUriParser.parse(uriStr)
                if (uriResult.isLeft()) {
                    return Either.Left(SipParseError.InvalidUri("Invalid URI in History-Info: ${uriResult.swap().getOrNull()?.message}"))
                }
                val uri = uriResult.getOrNull()!!
                
                // Parse parameters
                val parameters = mutableMapOf<String, Option<String>>()
                var index = "1"
                var rc: Option<String> = None
                
                if (paramsStr.isNotEmpty()) {
                    PARAM_REGEX.findAll(paramsStr).forEach { paramMatch ->
                        val (name, value) = paramMatch.destructured
                        when (name.trim()) {
                            "index" -> index = value.trim()
                            "rc" -> rc = Some(value.trim())
                            else -> parameters[name.trim()] = if (value.isNotEmpty()) Some(value.trim()) else None
                        }
                    }
                }
                
                entries.add(HistoryInfoEntry(uri, index, rc, parameters))
            }
            
            // Sort entries by index if needed
            val sortedEntries = entries.sortedBy { it.index }
            
            Either.Right(HistoryInfoHeader(sortedEntries))
            
        } catch (e: Exception) {
            log.error("Failed to parse History-Info header", e)
            Either.Left(SipParseError.UnknownError("Failed to parse History-Info header: ${e.message}"))
        }
    }

    override fun toString(obj: HistoryInfoHeader): String {
        val sb = StringBuilder()
        
        obj.entries.forEachIndexed { i, entry ->
            if (i > 0) {
                sb.append(", ")
            }
            
            // Format: <sip:uri>;index=1.1;rc=xyz;other=params
            sb.append("<").append(sipUriParser.toString(entry.uri)).append(">")
            
            // Add the index
            sb.append(";index=").append(entry.index)
            
            // Add reason code if present
            entry.rc.map { sb.append(";rc=").append(it) }
            
            // Add other parameters
            entry.parameters.forEach { (key, value) ->
                sb.append(";").append(key)
                value.map { sb.append("=").append(it) }
            }
        }
        
        return sb.toString()
    }
}
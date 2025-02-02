package org.daas.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import org.daas.dao.sip.SipObject
import org.daas.dao.sip.SipParseError
import org.daas.dao.sip.ContentTypeHeader


/**
 * Parser pour le header SIP Content-Type implémentant ISipParserProvider
 */
class ContentTypeParser : ISipParserProvider<ContentTypeHeader> {
    
    override fun fieldName(): String = "Content-Type"

    // Regex pour parser le type/subtype et la partie paramètres
    private val mainRegex = Regex("""^\s*([^/\s]+)\s*/\s*([^;\s]+)(?:\s*;\s*(.*))?$""")
    // Regex pour parser les paramètres key=value ou key seul
    private val paramRegex = Regex("""([^=;\s]+)(?:\s*=\s*(?:"([^"]*)"|([^";\s]+)))?""")
    
    /**
     * Parse la valeur du header Content-Type.
     * Exemples :
     * "application/sdp; charset=UTF-8"
     * "multipart/related; boundary=--myboundary; type=application/sdp"
     */
    override fun parse(message: String): Either<SipParseError, ContentTypeHeader> {
        if (message.isBlank()) {
            return Either.Left(SipParseError.EmptyMessage)
        }
        
        val mainMatch = mainRegex.find(message)
        if (mainMatch == null) {
            return Either.Left(SipParseError.InvalidFormat("Header is not in a valid format"))
        }
        
        val type = mainMatch.groupValues[1].trim()
        val subType = mainMatch.groupValues[2].trim()
        val paramsString = mainMatch.groupValues.getOrNull(3)?.trim() ?: ""
        
        val params = mutableMapOf<String, Option<String>>()
        if (paramsString.isNotEmpty()) {
            paramRegex.findAll(paramsString).forEach { matchResult ->
                val key = matchResult.groupValues[1].trim()
                // either group 2 (quoted) or group 3 (non-quoted)
                val value = matchResult.groupValues.getOrNull(2)?.ifEmpty { null }
                    ?: matchResult.groupValues.getOrNull(3)?.ifEmpty { null }
                if (value != null) {
                    params[key] = Some(value)
                } else {
                    params[key] = None
                }
            }
        }
        
        return Either.Right(ContentTypeHeader(type, subType, params))
    }
    
    /**
     * Sérialise l'objet ContentTypeHeader en une chaîne de caractères suivant la spécification SIP.
     */
    override fun toString(obj: ContentTypeHeader): String {
        val mainType = "${obj.type}/${obj.subType}"
        if (obj.parameters.isEmpty()) return mainType
        
        val paramString = obj.parameters.map { (k, v) ->
            when (v) {
                is Some -> "$k=${v.getOrElse { "" }}"
                else -> k
            }
        }.joinToString(separator = "; ", prefix = "; ")
        
        return mainType + paramString
    }
}
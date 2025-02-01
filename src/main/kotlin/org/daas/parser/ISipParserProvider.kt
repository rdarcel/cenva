package org.daas.parser

import org.daas.dao.sip.SipParseError
import org.daas.dao.sip.SipObject
import arrow.core.Either

/**
 * Interface to be implemented by the SIP parser
 */
interface ISipParserProvider<SipObjectType : SipObject> {
    /**
     * The SIP field name header (VIA, PAI, etc) or part of the request
     */
    fun fieldName(): String
    
    /**
     * Parse a SIP message
     * @param message the message to parse
     * @return the parsed message or an error if the message is invalid
     */
    fun parse(message: String): Either<SipParseError,SipObjectType>

    /**
     * Serialize the SIP object to a string
     */
    fun toString(obj : SipObjectType): String
}
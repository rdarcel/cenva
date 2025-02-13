package org.cenva.parser

import org.cenva.dao.sip.SipParseError
import org.cenva.dao.sip.SipObject
import arrow.core.Either

/**
 * Interface to be implemented by the SIP parser
 */
interface ISipParserProvider<SipObjectType : SipObject> {
 
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
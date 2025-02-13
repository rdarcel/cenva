package org.cenva.parser

import arrow.core.Either
import org.cenva.dao.sip.SipMethod
import org.cenva.dao.sip.SipParseError
import org.cenva.dao.sip.SipMethodValue

/**
 * Parser for SIP methods
 */
class SipMethodParser : ISipParserProvider<SipMethod> {
    
    /**
     * Parse a SIP method string into a SipMethod enum
     * @param message the method string to parse
     * @return Either a SipMethod or a SipParseError
     */
    override fun parse(message: String): Either<SipParseError, SipMethod> {
        return try {
            val method = SipMethodValue.values().find { it.name == message }
            if(method == null){
                Either.Left(SipParseError.InvalidFormat("Unknown SIP method: $message"))
            } else {
                Either.Right(SipMethod(method))
            }            
            
        } catch (e: Exception) {
            Either.Left(SipParseError.InvalidFormat("Invalid SIP method format: $message"))
        }
    }

    /**
     * Convert a SipMethod to its string representation
     * @param obj the SipMethod to convert
     * @return the string representation of the SipMethod
     */
    override fun toString(obj: SipMethod): String {
        return obj.toString()
    }
}
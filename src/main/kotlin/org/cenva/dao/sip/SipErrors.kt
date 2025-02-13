package org.cenva.dao.sip

import arrow.core.Option

/**
 * SIP parse error
 */
sealed class SipParseError : Throwable() {
    /* 
    * Invalid format error
     * @param message the error message
     */
    data class InvalidFormat(override val message: String) : SipParseError()

    /* 
    * Invalid format Uri
     * @param message the error message
     */
    data class InvalidUri(override val message: String) : SipParseError()

    /* 
    * Invalid format Uri
     * @param message the error message
     */
    data class MissingField(override val message: String) : SipParseError()

    /**
     * Empty message error
     */
    data class EmptyMessage(override val message: String) : SipParseError()

    /**
     * Handle Multiple errors
     */
    class MultipleError(errors: List<SipParseError>) : SipParseError(){
        init{
            var msg = ""
            errors.forEach { msg += it.message + " | " }
        }
    }
    
    /**
     * Unknow message error
     */
    data class UnknownError(override val message: String) : SipParseError()

 
}

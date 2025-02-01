package org.daas.dao.sip

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
    
    /**
     * Empty message error
     */
    object EmptyMessage : SipParseError()
}

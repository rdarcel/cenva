package org.daas.dao

import arrow.core.Option

/** SIP message Definition */
sealed class SipMessage

/**
 * SIP Request
 * @param method the method of the request
 * @param requestUri the request URI
 * @param sipVersion the SIP version
 * @param headers the headers of the request
 * @param sdpContent the SDP content of the request
 */
data class SipRequest(
        val method: String,
        val requestUri: String,
        val sipVersion: String,
        val headers: Map<String, List<String>>,
        val sdpContent: String?
) : SipMessage()

/**
 * SIP Response
 * @param sipVersion the SIP version
 * @param statusCode the status code of the response
 * @param reasonPhrase the reason phrase of the response
 * @param headers the headers of the response
 * @param sdpContent the SDP content of the response
 */
data class SipResponse(
        val sipVersion: String,
        val statusCode: Int,
        val reasonPhrase: String,
        val headers: Map<String, List<String>>,
        val sdpContent: String?
) : SipMessage()

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

/**
 * Class to be implemented by all SIP objects
 */
sealed class SipObject

/**
 * Class representing all the possible Sip methods
 */
enum class SipMethod {
    INVITE,
    ACK,
    BYE,
    CANCEL,
    OPTIONS,
    REGISTER,
    PRACK,
    SUBSCRIBE,
    NOTIFY,
    PUBLISH,
    INFO,
    REFER,
    MESSAGE,
    UPDATE;

    override fun toString(): String {
        return name
    }
}

/**
 * SIP URI as defined in RFC 3261
 * @param scheme The scheme (typically "sip" or "sips")
 * @param userInfo The user info part (optional)
 * @param host The host part
 * @param port The port (optional)
 * @param uriParams The URI parameters
 * @param headers The headers parameters
 */
data class SipUri(
    val scheme: String,
    val userInfo: Option<String>,
    val host: String,
    val port: Option<Int>,
    val uriParams: Map<String, Option<String>>,
    val headers: Map<String, String>
) : SipObject()
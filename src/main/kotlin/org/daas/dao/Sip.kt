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

    /**
     * Transform method to String
     */
    override fun toString(): String {
        return name
    }

    companion object {
        /**
         * Get a SipMethod from a string
         * @param method the string to parse
         * @return the SipMethod
         */
        fun fromString(method: String): SipMethod? {
            return values().find { it.name == method }
        }
    }
}

/**
 * Class representing a SIP URI
 * @param scheme the scheme of the URI
 * @param userInfo the user information
 * @param password Optional password of the user
 * @param host the host
 * @param port the port
 * @param uriParameters the URI parameters
 * @param headers the headers
 * @param phoneContext the phone context
 * @param postDial the post dial 
 * @param isdnSubaddress the ISDN subaddress
 * 
 */
data class SipUri(
    val scheme: String,
    val userInfo: Option<String>, 
    val password: Option<String>,
    val host: Option<String>, 
    val port: Option<Int>, 
    val uriParameters: Map<String, Option<String>>,
    val headers: Map<String, String>
) : SipObject()

/**
 * Represents a name-addr header field as defined in RFC 3261
 * @param displayName Optional display name
 * @param uri The SIP URI
 * @param parameters Optional parameters
 */
data class NameAddr(
    val displayName: Option<String>,
    val uri: SipUri
) : SipObject()

/**
 * Represents a From header field value as defined in RFC 3261
 * @param nameAddr The name-addr or addr-spec part
 * @param tag Optional tag parameter
 * @param parameters Additional header parameters
 */
data class FromHeader(
    val nameAddr: NameAddr,
    val tag: Option<String>,
    val parameters: Map<String, Option<String>>
) : SipObject()
package org.daas.dao.sip

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


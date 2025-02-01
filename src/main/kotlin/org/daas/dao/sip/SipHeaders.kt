package org.daas.dao.sip

import arrow.core.Option

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
 * Represents a header Adress field as defined in RFC 3261, could contains From, To & Contact Header
 * @param nameAddr The name-addr field
 * @param tag Optional tag support
 * @param parameters Optional parameters set in adress
 */
data class AddressHeader(
    val nameAddr: NameAddr,
    val tag: Option<String>,
    val parameters: Map<String, Option<String>>
) : SipObject()

/**
 * Represents a From header field value as defined in RFC 3261
    * @param addr The name-addr field
 */
data class FromHeader(
    val address: AddressHeader
) :SipObject()

/**
 * Represents a From header field value as defined in RFC 3261
    * @param addr The name-addr field
 */
data class ToHeader(
    val address: AddressHeader
) :SipObject()

/**
 * Represents a From header field value as defined in RFC 3261
    * @param addr The name-addr field
 */
data class ContactHeader(
    val address: AddressHeader
) :SipObject()

/**
 * Represents a CSeq header field value as defined in RFC 3261
 * @param sequenceNumber The sequence number
 * @param method The SIP method
 */
data class CSeqHeader(
    val sequenceNumber: Long,
    val method: SipMethod
) : SipObject()

/**
 * Represents a Call-ID header field value as defined in RFC 3261
 * @param identifier The local identifier
 * @param host Optional host where the identifier was created
 */
data class CallId(
    val identifier: String,
    val host: Option<String>
) : SipObject()


/**
 * Represents a Via header field as defined in RFC 3261
 * @param protocol The protocol used (e.g., "SIP/2.0")
 * @param transport The transport protocol (e.g., "UDP", "TCP")
 * @param host The host address
 * @param port Optional port number
 * @param parameters Optional Via parameters (branch, received, rport, etc.)
 */
data class ViaHeader(
    val protocol: String,
    val transport: String,
    val host: String,
    val port: Option<Int>,
    val parameters: Map<String, Option<String>>,
    val branch: Option<String>
) : SipObject()

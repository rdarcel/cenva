package org.cenva.dao.sip

import arrow.core.Option
import arrow.core.None
import arrow.core.Either
import arrow.core.Some
import arrow.core.getOrElse
import arrow.optics.*
import kotlin.collections.listOf
import org.cenva.dao.sip.SipParseError


/** SIP message Definition */
sealed class SipMessageRaw

/**
 * SIP Request
 * @param method the method of the request
 * @param requestUri the request URI
 * @param sipVersion the SIP version
 * @param headers the headers of the request
 * @param sdpContent the SDP content of the request
 */
data class SipRequestRaw(
        val method: String,
        val requestUri: String,
        val sipVersion: String,
        val headers: Map<String, List<String>>,
        val sdpContent: String?
) : SipMessageRaw()

/**
 * SIP Response
 * @param sipVersion the SIP version
 * @param statusCode the status code of the response
 * @param reasonPhrase the reason phrase of the response
 * @param headers the headers of the response
 * @param sdpContent the SDP content of the response
 */
data class SipResponseRaw(
        val sipVersion: String,
        val statusCode: Int,
        val reasonPhrase: String,
        val headers: Map<String, List<String>>,
        val sdpContent: String?
) : SipMessageRaw()


/**
 * Class to be implemented by all SIP objects
 */
sealed class SipObject

/**
 * Class representing a SIP method
 */
data class SipMethod(val method: SipMethodValue): SipObject()

/**
 * Class representing all the possible Sip methods
 */
enum class SipMethodValue  {
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
    UPDATE
}

/** 
 * Class representing a SIP version
 */
class SipVersion(val version: SipVersionValue): SipObject()

/**
 * List all sip version possible values
 */
enum class SipVersionValue{
    SIP_2_0
}


/**
 * Class representing the a Sip Message (inherited by Sip Request & SIP Response)
 */
abstract class SipMessage(
val sipVersion: SipVersion,
val via: List<ViaHeader>,
val maxForwards: Int,
val from: FromHeader,
val to: ToHeader,
val callId: CallIdHeader,
val cSeq: CSeqHeader,
val contact: List<ContactHeader>,
val contentTypeHeader: Option<ContentTypeHeader>,
val contentLength: Option<Int>,
val headers: Map<String, List<String>>,
val body : List<String>){
    abstract class SipMessageBuilder(){

        /**
         * SIP version
         */
        protected lateinit var _sipVersion: SipVersion

        /**
         * Via header
         */
        protected lateinit var _via: List<ViaHeader>

        /**
         * Max-Forwards header
         */
        protected var _maxForwards: Int = -1

        /**
         * From header
         */
        protected lateinit var _from: FromHeader 

        /**
         * To header
         */
        protected lateinit var _to: ToHeader 

        /**
         * Call-ID header
         */
        protected lateinit var _callId: CallIdHeader

        /**
         * CSeq header
         */
        protected lateinit var _cSeq: CSeqHeader 

        /**
         * Contact header
         */
        protected var _contact: Option<List<ContactHeader>> = None

        /**
         * Content-Type header
         */
        protected var _contentTypeHeader: Option<ContentTypeHeader> = None

        /**
         * Content-Length header
         */
        protected var _contentLength: Option<Int> = None

        /**
         * Headers
         */
        protected var _headers: MutableMap<String, List<String>> = mutableMapOf()

        /**
         * Body
         */
        protected var _body: Option<List<String>> = None

        /**
         * Errors added through the different mutation operations
         */
        protected val errors: MutableList<SipParseError> = mutableListOf()

        /**
         * Set the sip version
         * @param sipVersion the sip version or an error
         */
        fun sipVersion(sipVersion: Either<SipParseError, SipVersion>) = sipVersion.fold({errors.add(it)},{_sipVersion = it})

        /**
         * Set the via header
         * @param via the via header or an error
         */
        fun via(via: Either<SipParseError, ViaHeader>) = via.fold({errors.add(it)},{this.addVia(it)})

        /**
         * Set the max forwards header
         * @param maxForwards the max forwards header or an error
         */
        fun maxForwards(maxForwards: Either<SipParseError, Int>) = maxForwards.fold({errors.add(it)},{_maxForwards = it})

        /**
         * Set the from header
         * @param from the from header or an error
         */
        fun from(from: Either<SipParseError, FromHeader>) = from.fold({ errors.add(it)},{_from = it})

        /**
         * Set the to header
         * @param to the to header or an error
         */
        fun to(to: Either<SipParseError, ToHeader>) = to.fold({errors.add(it)},{_to = it})

        /**
         * Set the call id header
         * @param callId the call id header or an error
         */
        fun callId(callId: Either<SipParseError, CallIdHeader>) = callId.fold({errors.add(it)},{_callId = it})

        /**
         * Set the cseq header
         * @param cSeq the cseq header or an error
         */
        fun cSeq(cSeq: Either<SipParseError, CSeqHeader>) = cSeq.fold({errors.add(it)},{_cSeq = it})

        /**
         * Set the contact header
         * @param contact the contact header or an error
         */
        fun contact(contact: Either<SipParseError, ContactHeader>) = contact.fold({errors.add(it)},{this.addContact(it)})

        /**
         * Set the content type header
         * @param contentTypeHeader the content type header or an error
         */
        fun contentType(contentTypeHeader: Either<SipParseError, ContentTypeHeader>) = contentTypeHeader.fold({_contentTypeHeader = None; errors.add(it)},{_contentTypeHeader = Some(it)})

        /**
         * Set the content length
         * @param contentLength the content length or an error
         */
        fun contentLength(contentLength: Either<SipParseError, Int>) = contentLength.fold({_contentLength = None; errors.add(it)},{_contentLength = Some(it)})

        /**
         * Add an headers
         * @param headerName the name of the header
         * @param headerValue the value of the header
         */
        fun header(headerName: String, headerValue:String){
            if(!this._headers.containsKey(headerName)){
                this._headers.put(headerName, listOf())
            }
            this._headers.get(headerName)?.plus(headerValue)
        }

        /**
         *  Add a via header
         * @param via the via header
         */
        fun addVia(via:ViaHeader){
            if(this::_via.isInitialized){
                this._via.plus(via)
            } else{
                this._via = listOf(via)
            }
        }

        /**
         * Add a new contact
         * @param contact the contact to add
         */
        fun addContact(contact:ContactHeader){
            if(this._contact.isNone()){
                this._contact = Some(listOf(contact))
            } else{
                this._contact.map { it.plus(contact) }
            }
        }

        /**
         * Pre build check for all types of sip messages
         * @return an option of an error 
         */
        fun preBuild():Option<SipParseError>{
        
            if (!this::_sipVersion.isInitialized) {
                errors.add(SipParseError.MissingField("Missing mandatory field: sipVersion"))
            }
            if (!this::_via.isInitialized) {
                errors.add(SipParseError.MissingField("Missing mandatory field: via"))
            }
            if (this._maxForwards < 0) {
                errors.add(SipParseError.MissingField("Missing mandatory field: Max-forwards "+this._maxForwards))
            }
            if (!this::_from.isInitialized) {
                errors.add(SipParseError.MissingField("Missing mandatory field: from"))
            }
            if (!this::_to.isInitialized) {
                errors.add(SipParseError.MissingField("Missing mandatory field: to"))
            }
            if (!this::_callId.isInitialized) {
                errors.add(SipParseError.MissingField("Missing mandatory field: callId"))
            }
            if (!this::_cSeq.isInitialized) {
                errors.add(SipParseError.MissingField("Missing mandatory field: cSeq"))
            }
            if(errors.isNotEmpty()){
                return if(errors.size > 1) Some(SipParseError.MultipleError(errors)) else Some(errors[0])
            }
            return None
        }

        /* 
        * Add a body to the message
        */
        fun addBody(body:String){
            if(this._body.isNone()){
                this._body = Some(listOf(body))
            } else{
                this._body.map { it.plus(body) }
            }
        }


    }
}

/**
 * SIP Request
 * @param sipMethod the method of the request
 * @param sipVersion the SIP version
 * @param via the Via header
 * @param maxForwards the Max-Forwards header
 * @param from the From header
 * @param to the To header
 * @param callId the Call-ID header
 * @param cSeq the CSeq header
 * @param contact the Contact header
 * @param contentTypeHeader the Content-Type header
 * @param contentLength the Content-Length header
 * @param headers the headers of the request
 * @param body the body of the request
 */
class SipRequest(
    val method:SipMethod,
    val uri: SipUri,
    sipVersion: SipVersion,
    via: List<ViaHeader>,
    maxForwards: Int,
    from: FromHeader,
    to: ToHeader,
    callId: CallIdHeader,
    cSeq: CSeqHeader,
    contact: List<ContactHeader>,
    contentTypeHeader: Option<ContentTypeHeader>,
    contentLength: Option<Int>,
    headers: Map<String, List<String>>,
    body : List<String>
):SipMessage(sipVersion, via, maxForwards, from, to, callId, cSeq, contact, contentTypeHeader, contentLength, headers, body){
 
    /**
     * SIP Request Builder to build from a decomposed Sip request
     */
    class SipRequestBuilder: SipMessageBuilder(){
        /**
         * SIP method
         */
        protected lateinit var _sipMethod: SipMethod

        /**
         * SIP URI
         */
        protected lateinit var _uri: SipUri

        /**
         * Set the sip method
         * @param sipMethod the sip method or an error
         */
        fun sipMethod(sipMethod: Either<SipParseError, SipMethod>) = sipMethod.fold({ errors.add(it)},{_sipMethod = it})

        /**
         * Set the uri
         * @param uri the uri or an error
         */
        fun uri(uri: Either<SipParseError, SipUri>) = uri.fold({errors.add(it)},{_uri = it})

        /**
         * Build the SipRequest
         * @return the SipRequest or an error
         */
        fun build(): Either<SipParseError, SipRequest> {
            this.preBuild()
  
            if(!this::_sipMethod.isInitialized){
                errors.add(SipParseError.MissingField("Missing mandatory field in the SIP request (method)"))
            } 
            if(!this::_uri.isInitialized){
                errors.add(SipParseError.MissingField("Missing mandatory field in the SIP request (uri)"))
            }
            //If there are some errors return an aggregated error
            if(errors.isNotEmpty()){
                return Either.Left(SipParseError.MultipleError(errors))
            }

            return Either.Right(SipRequest(_sipMethod, _uri, _sipVersion, _via, _maxForwards, _from, _to, _callId, _cSeq, _contact.getOrElse { listOf() }, _contentTypeHeader, _contentLength, _headers, _body.getOrElse { listOf() }))
        }
    
    }
}

/**
 * SIP Response
 * @param sipVersion the SIP version
 * @param statusCode the status code of the response
 * @param reasonPhrase the reason phrase of the response
 * @param via the Via header
 * @param maxForwards the Max-Forwards header
 * @param from the From header
 * @param to the To header
 * @param callId the Call-ID header
 * @param cSeq the CSeq header
 * @param contact the Contact header
 * @param contentTypeHeader the Content-Type header
 * @param contentLength the Content-Length header
 * @param headers the headers of the response
 * @param body the body of the response
 */
class SipResponse(
    val statusCode: Int,
    val reasonPhrase: Option<String>,
    sipVersion: SipVersion,
    via: List<ViaHeader>,
    maxForwards: Int,
    from: FromHeader,
    to: ToHeader,
    callId: CallIdHeader,
    cSeq: CSeqHeader,
    contact: List<ContactHeader>,
    contentTypeHeader: Option<ContentTypeHeader>,
    contentLength: Option<Int>,
    headers: Map<String, List<String>>,
    body : List<String>
):SipMessage(sipVersion, via, maxForwards, from, to, callId, cSeq, contact, contentTypeHeader, contentLength, headers, body){

    /**
     * SIP Response Builder
     */
    class SipResponseBuilder: SipMessageBuilder(){
        /**
         * Status code
         */
        protected var _statusCode: Int = -1

        /**
         * Reason phrase
         */
        protected var _reasonPhrase: Option<String> = None

        /**
         * Set the status code
         * @param statusCode the status code or an error
         */
        fun statusCode(statusCode: Either<SipParseError, Int>) = statusCode.fold({errors.add(it)},{_statusCode = it})

        /**
         * Set the reason phrase
         * @param reasonPhrase the reason phrase or an error
         */
        fun reasonPhrase(reasonPhrase: Either<SipParseError, String>) = reasonPhrase.fold({errors.add(it)},{_reasonPhrase = Some(it)})

        /**
         * Build the SipResponse
         * @return the SipResponse or an error
         */
        fun build(): Either<SipParseError, SipResponse> {
            this.preBuild()

            if(this._statusCode < 0){
                errors.add(SipParseError.MissingField("Missing mandatory field in the SIP response (status code)"))
            }
            /**
            if(!this::_reasonPhrase.isInitialized){
                errors.add(SipParseError.MissingField("Missing mandatory field in the SIP response (reason phrase)"))
            }**/

            //If there are some errors return an aggregated error
            if(errors.isNotEmpty()){
                return Either.Left(SipParseError.MultipleError(errors))
            }
            return Either.Right(SipResponse(_statusCode, _reasonPhrase, _sipVersion, _via, _maxForwards, _from, _to, _callId, _cSeq, _contact.getOrElse { listOf() }, _contentTypeHeader, _contentLength, _headers, _body.getOrElse { listOf() }))
        }
    }
}

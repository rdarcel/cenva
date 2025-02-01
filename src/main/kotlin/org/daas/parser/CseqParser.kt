package org.daas.parser

import arrow.core.Either
import org.daas.dao.sip.SipMethod
import org.daas.dao.sip.SipObject
import org.daas.dao.sip.SipParseError
import org.daas.dao.sip.CSeqHeader
import org.jboss.logging.Logger


/**
 * Parser for CSeq header fields as specified in RFC 3261
 */
class CSeqParser : ISipParserProvider<CSeqHeader> {

    companion object {
        private val log = Logger.getLogger(CSeqParser::class.java)
        
        // CSeq format: sequence-number LWS Method
        private val CSEQ_REGEX = """^\s*(\d+)\s+([A-Z][A-Z0-9]*)\s*$""".toRegex()
    }

    override fun fieldName(): String = "CSeq"

    /**
     * Parse a CSeq header field
     * @param message the CSeq header field to parse
     * @return Either an error or a CSeqHeader object
     */
    override fun parse(message: String): Either<SipParseError, CSeqHeader> {
        val match = CSEQ_REGEX.find(message) 
            ?: return Either.Left(SipParseError.InvalidFormat("Invalid CSeq format: $message"))

        val (seqNumStr, methodStr) = match.destructured

        try {
            val sequenceNumber = seqNumStr.toLong()
            if(sequenceNumber > 2147483648) {
                return Either.Left(SipParseError.InvalidFormat("Invalid sequence number (bigger than 2^31) in CSeq: $seqNumStr"))
            }
            val method = SipMethod.fromString(methodStr) 
                ?: return Either.Left(SipParseError.InvalidFormat("Invalid method in CSeq: $methodStr"))

            return Either.Right(CSeqHeader(sequenceNumber, method))
        } catch (e: NumberFormatException) {
            return Either.Left(SipParseError.InvalidFormat("Invalid sequence number in CSeq: $seqNumStr"))
        }
    }

    /**
     * Convert a CSeqHeader object to a string
     * @param obj the CSeqHeader object to convert
     * @return the string representation of the CSeqHeader
     */
    override fun toString(obj: CSeqHeader): String {
        return "${obj.sequenceNumber} ${obj.method}"
    }
}
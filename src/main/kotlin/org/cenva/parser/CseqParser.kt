package org.cenva.parser

import arrow.core.Either
import org.cenva.dao.sip.SipMethod
import org.cenva.dao.sip.SipObject
import org.cenva.dao.sip.SipParseError
import org.cenva.dao.sip.CSeqHeader
import org.jboss.logging.Logger


/**
 * Parser for CSeq header fields as specified in RFC 3261
 */
class CSeqParser(private val sipMethodParser : SipMethodParser) : ISipParserProvider<CSeqHeader> {

    companion object {
        private val log = Logger.getLogger(CSeqParser::class.java)
        
        // CSeq format: sequence-number LWS Method
        private val CSEQ_REGEX = """^\s*(\d+)\s+([A-Z][A-Z0-9]*)\s*$""".toRegex()
    }

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
            val method = sipMethodParser.parse(methodStr) 
            return when(method) {
                is Either.Left -> return Either.Left(SipParseError.InvalidFormat("Invalid method in CSeq: $methodStr"))
                is Either.Right -> Either.Right(CSeqHeader(sequenceNumber, method.value))
            }
                 
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
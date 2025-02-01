package org.daas

import io.quarkus.test.junit.QuarkusTest
import org.daas.dao.sip.CSeqHeader
import org.daas.dao.sip.SipMethod
import org.daas.parser.CSeqParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@QuarkusTest
class TestParserSipCSeq {

    private val cseqParser = CSeqParser()

    /**
     * Test basic CSeq header with INVITE method
     * Example from RFC 3261 Section 8.1.1.5
     */
    @Test
    fun testBasicCSeqInvite() {
        val result = cseqParser.parse("1 INVITE")
        assertTrue(result.isRight())
        result.map { 
            assertEquals(1L, it.sequenceNumber)
            assertEquals(SipMethod.INVITE, it.method)
        }
    }

    /**
     * Test CSeq header with large sequence number
     * Testing boundary conditions
     */
    @Test
    fun testLargeSequenceNumber() {
        val result = cseqParser.parse("2147483647 REGISTER")
        assertTrue(result.isRight())
        result.map {
            assertEquals(2147483647L, it.sequenceNumber)
            assertEquals(SipMethod.REGISTER, it.method)
        }
    }

    /**
     * Test CSeq header with multiple spaces between components
     * Testing parser resilience to excessive whitespace
     */
    @Test
    fun testMultipleSpaces() {
        val result = cseqParser.parse("42     SUBSCRIBE")
        assertTrue(result.isRight())
        result.map {
            assertEquals(42L, it.sequenceNumber)
            assertEquals(SipMethod.SUBSCRIBE, it.method)
        }
    }

    /**
     * Test CSeq header with invalid sequence number
     * Testing error handling for malformed input
     */
    @Test
    fun testInvalidSequenceNumber() {
        val result = cseqParser.parse("abc INVITE")
        assertTrue(result.isLeft())
    }

    /**
     * Test CSeq header with unknown method
     * Testing handling of non-standard SIP methods
     */
    @Test
    fun testUnknownMethod() {
        val result = cseqParser.parse("1 UNKNOWN")
        assertTrue(result.isLeft())
    }

    /**
     * Test CSeq header with empty method
     * Testing error handling for missing method
     */
    @Test
    fun testEmptyMethod() {
        val result = cseqParser.parse("1 ")
        assertTrue(result.isLeft())
    }

    /**
     * Test CSeq header with BYE method
     * Common use case in SIP dialogs
     */
    @Test
    fun testByeMethod() {
        val result = cseqParser.parse("2 BYE")
        assertTrue(result.isRight())
        result.map {
            assertEquals(2L, it.sequenceNumber)
            assertEquals(SipMethod.BYE, it.method)
        }
    }

    /**
     * Test CSeq header with zero sequence number
     * Edge case testing
     */
    @Test
    fun testZeroSequenceNumber() {
        val result = cseqParser.parse("0 OPTIONS")
        assertTrue(result.isRight())
        result.map {
            assertEquals(0L, it.sequenceNumber)
            assertEquals(SipMethod.OPTIONS, it.method)
        }
    }

    /**
     * Test CSeq header with CANCEL method
     * Important use case for canceling pending requests
     */
    @Test
    fun testCancelMethod() {
        val result = cseqParser.parse("1493 CANCEL")
        assertTrue(result.isRight())
        result.map {
            assertEquals(1493L, it.sequenceNumber)
            assertEquals(SipMethod.CANCEL, it.method)
        }
    }

    /**
     * Test CSeq header without sequence number
     * Testing error handling for missing sequence number
     */
    @Test
    fun testMissingSequenceNumber() {
        val result = cseqParser.parse("INVITE")
        assertTrue(result.isLeft())
    }

    /**
     * Test CSeq header with sequence number larger than 2^31
     * Testing error handling for sequence number overflow
     */
    @Test
    fun testSequenceNumberOverflow() {
        val result = cseqParser.parse("2147483649 INVITE")
        assertTrue(result.isLeft(), "Should fail on sequence number overflow ${result.map{it.sequenceNumber}}")
    }
}
package org.daas

import arrow.core.None
import arrow.core.Some
import io.quarkus.test.junit.QuarkusTest
import org.cenva.dao.sip.HistoryInfoEntry
import org.cenva.dao.sip.HistoryInfoHeader
import org.cenva.parser.HistoryInfoParser
import org.cenva.parser.SipUriParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for HistoryInfoParser according to RFC 7131
 */
@QuarkusTest
class TestParserHistoryInfo {

    private val historyInfoParser = HistoryInfoParser(SipUriParser())

    /**
     * Test parsing a simple History-Info header with a single entry
     */
    @Test
    fun testBasicHistoryInfoParsing() {
        val header = "<sip:alice@atlanta.example.com>;index=1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(1, historyInfo.entries.size)
            assertEquals("1", historyInfo.entries[0].index)
            assertEquals("sip", historyInfo.entries[0].uri.scheme)
            assertEquals(Some("alice"), historyInfo.entries[0].uri.userInfo)
            assertEquals(Some("atlanta.example.com"), historyInfo.entries[0].uri.host)
            assertEquals(None, historyInfo.entries[0].rc)
        }
    }

    /**
     * Test parsing a History-Info header with multiple entries
     */
    @Test
    fun testMultipleEntriesHistoryInfo() {
        val header = "<sip:bob@biloxi.example.com>;index=1, <sip:alice@atlanta.example.com>;index=1.1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(2, historyInfo.entries.size)
            assertEquals("1", historyInfo.entries[0].index)
            assertEquals("1.1", historyInfo.entries[1].index)
            assertEquals(Some("bob"), historyInfo.entries[0].uri.userInfo)
            assertEquals(Some("alice"), historyInfo.entries[1].uri.userInfo)
        }
    }

    /**
     * Test parsing a History-Info header with reason code (rc) parameter
     */
    @Test
    fun testHistoryInfoWithReasonCode() {
        val header = "<sip:bob@biloxi.example.com>;index=1;rc=302"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(1, historyInfo.entries.size)
            assertEquals(Some("302"), historyInfo.entries[0].rc)
        }
    }

    /**
     * Test parsing a History-Info header with custom parameters
     */
    @Test
    fun testHistoryInfoWithCustomParameters() {
        val header = "<sip:carol@chicago.example.com>;index=1;mp=1;np=2"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(1, historyInfo.entries.size)
            assertEquals("1", historyInfo.entries[0].index)
            assertEquals(Some("1"), historyInfo.entries[0].parameters["mp"])
            assertEquals(Some("2"), historyInfo.entries[0].parameters["np"])
        }
    }

    /**
     * Test parsing a History-Info header with URI parameters and headers
     */
    @Test
    fun testHistoryInfoWithUriParameters() {
        val header = "<sip:bob@biloxi.example.com;transport=tcp?subject=meeting>;index=1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(1, historyInfo.entries.size)
            assertEquals(Some("tcp"), historyInfo.entries[0].uri.uriParameters["transport"])
            assertEquals("meeting", historyInfo.entries[0].uri.headers["subject"])
        }
    }

    /**
     * Test serialization of a simple History-Info header
     */
    @Test
    fun testHistoryInfoSerialization() {
        val header = "<sip:alice@atlanta.example.com>;index=1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            val serialized = historyInfoParser.toString(historyInfo)
            assertEquals(header, serialized)
        }
    }

    /**
     * Test serialization of a complex History-Info header
     */
    @Test
    fun testComplexHistoryInfoSerialization() {
        val header = "<sip:bob@biloxi.example.com>;index=1, <sip:alice@atlanta.example.com>;index=1.1;rc=302"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            val serialized = historyInfoParser.toString(historyInfo)
            assertEquals(header, serialized)
        }
    }

    /**
     * Test parsing history info entries that are out of order and verify proper sorting
     */
    @Test
    fun testHistoryInfoSorting() {
        val header = "<sip:carol@chicago.example.com>;index=2, <sip:bob@biloxi.example.com>;index=1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(2, historyInfo.entries.size)
            assertEquals("1", historyInfo.entries[0].index)
            assertEquals("2", historyInfo.entries[1].index)
            assertEquals(Some("bob"), historyInfo.entries[0].uri.userInfo)
            assertEquals(Some("carol"), historyInfo.entries[1].uri.userInfo)
        }
    }

    /**
     * Test a more complex nested index structure like those in the RFC example
     */
    @Test
    fun testComplexIndexStructure() {
        val header = "<sip:user@example.com>;index=1, " +
                "<sip:user1@example.com>;index=1.1, " +
                "<sip:user2@example.com>;index=1.2, " +
                "<sip:user3@example.com>;index=1.1.1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(4, historyInfo.entries.size)
            assertEquals("1", historyInfo.entries[0].index)
            assertEquals("1.1", historyInfo.entries[1].index)
            assertEquals("1.1.1", historyInfo.entries[3].index)
            assertEquals("1.2", historyInfo.entries[2].index)
        }
    }

    /**
     * Test parsing a History-Info header from a call forwarding scenario in RFC 7131
     */
    @Test
    fun testCallForwardingScenario() {
        val header = "<sip:alice@atlanta.example.com>;index=1, " +
                "<sip:bob@biloxi.example.com>;index=1.1;rc=302, " +
                "<sip:carol@chicago.example.com>;index=1.1.1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(3, historyInfo.entries.size)
            assertEquals("1", historyInfo.entries[0].index)
            assertEquals("1.1", historyInfo.entries[1].index)
            assertEquals(Some("302"), historyInfo.entries[1].rc)
            assertEquals("1.1.1", historyInfo.entries[2].index)
        }
    }

    /**
     * Test error handling for invalid History-Info format
     */
    @Test
    fun testInvalidHistoryInfoFormat() {
        val invalidHeader = "sip:bob@biloxi.example.com;index=1" // Missing angle brackets
        
        val result = historyInfoParser.parse(invalidHeader)
        assertTrue(result.isLeft())
        
    }

    /**
     * Test error handling for a History-Info entry with invalid URI
     */
    @Test
    fun testHistoryInfoWithInvalidUri() {
        val invalidHeader = "<invalid:uri>;index=1"
        
        val result = historyInfoParser.parse(invalidHeader)
        assertTrue(result.isLeft())
    }

    /**
     * Test parsing a History-Info header with SIPS URI
     */
    @Test
    fun testHistoryInfoWithSipsUri() {
        val header = "<sips:alice@secure.example.com>;index=1"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(1, historyInfo.entries.size)
            assertEquals("sips", historyInfo.entries[0].uri.scheme)
            assertEquals(Some("alice"), historyInfo.entries[0].uri.userInfo)
            assertEquals(Some("secure.example.com"), historyInfo.entries[0].uri.host)
        }
    }

    /**
     * Test parsing a History-Info header with an empty parameter
     */
    @Test
    fun testHistoryInfoWithEmptyParameter() {
        val header = "<sip:alice@atlanta.example.com>;index=1;empty"
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(1, historyInfo.entries.size)
            assertEquals(None, historyInfo.entries[0].parameters["empty"])
        }
    }

    /**
     * Test parsing a History-Info header with a parameter that has an empty value
     */
    @Test
    fun testHistoryInfoWithEmptyValueParameter() {
        val header = "<sip:alice@atlanta.example.com>;index=1;param="
        
        val result = historyInfoParser.parse(header)
        assertTrue(result.isRight())
        
        result.map { historyInfo ->
            assertEquals(1, historyInfo.entries.size)
            assertEquals(None, historyInfo.entries[0].parameters["param"])
        }
    }
}
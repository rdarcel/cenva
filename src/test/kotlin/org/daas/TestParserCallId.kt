package org.cenva

import io.quarkus.test.junit.QuarkusTest
import org.cenva.parser.CallIdParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import arrow.core.Some
import arrow.core.None

/**
 * Test the Call-ID parser
 */
@QuarkusTest
class TestParserCallId {

    private val parser = CallIdParser()

    /**
     * Test basic Call-ID header field without host
     */
    @Test
    fun testBasicCallId() {
        val input = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
        val result = parser.parse(input)
        
        assertTrue(result.isRight(), "Parsing Call-IDwithout host")
        result.map { callId ->
            assertEquals("f81d4fae-7dec-11d0-a765-00a0c91e6bf6", callId.identifier)
            assertEquals(None, callId.host)
        }
    }

    /**
     * Test Call-ID header field with host
     */
    @Test
    fun testCallIdWithHost() {
        val input = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6@example.com"
        val result = parser.parse(input)
        
        assertTrue(result.isRight(),"Testing parsing of call id with host")
        result.map { callId ->
            assertEquals("f81d4fae-7dec-11d0-a765-00a0c91e6bf6", callId.identifier)
            assertEquals(Some("example.com"), callId.host)
        }
    }

    /** 
     * Test Call-ID header field with special characters
     */
    @Test
    fun testCallIdWithSpecialChars() {
        val input = "abc123.DEFGH_!%+-=~"
        val result = parser.parse(input)
        
        assertTrue(result.isRight(),"Testing parsing of call id with special characters")
        result.map { callId ->
            assertEquals("abc123.DEFGH_!%+-=~", callId.identifier)
            assertEquals(None, callId.host)
        }
    }

    /** 
     * Test Call-ID header field with empty identifier, it should fails
     */
    @Test
    fun testEmptyCallId() {
        val input = ""
        val result = parser.parse(input)
        assertTrue(result.isLeft(),"Testing parsing of call id with an empty string => it should fails")
    }

    /**
     * Test reserialization of call id
     */
    @Test 
    fun testToString() {
        val callIdWithHost = parser.parse("1234@example.com").getOrNull()!!
        assertEquals("1234@example.com", parser.toString(callIdWithHost),"Testing serialization of call id with host")

        val callIdNoHost = parser.parse("1234").getOrNull()!!
        assertEquals("1234", parser.toString(callIdNoHost), "Testing serialization of call id without host")
    }

}
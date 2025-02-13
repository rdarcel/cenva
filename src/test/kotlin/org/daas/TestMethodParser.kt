package org.daas

import io.quarkus.test.junit.QuarkusTest
import org.cenva.parser.SipMethodParser
import org.cenva.dao.sip.SipMethodValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Test the method parser
 */
@QuarkusTest
class TestMethodParser {
    
    private val methodParser = SipMethodParser()

    /**
     * Test all valid SIP methods from RFC 3261
     */
    @Test
    fun testValidMethods() {
        val methods = listOf(
            "INVITE",
            "ACK",
            "BYE",
            "CANCEL",
            "OPTIONS",
            "REGISTER"
        )

        methods.forEach { method ->
            val result = methodParser.parse(method)
            assertTrue(result.isRight())
            result.map {
                assertEquals(SipMethodValue.valueOf(method), it.method)
            }
        }
    }

    /**
     * Test extended SIP methods defined in other RFCs
     */
    @Test
    fun testExtendedMethods() {
        val extendedMethods = listOf(
            "SUBSCRIBE",
            "NOTIFY",
            "PUBLISH",
            "INFO",
            "REFER",
            "MESSAGE",
            "UPDATE",
            "PRACK"
        )

        extendedMethods.forEach { method ->
            val result = methodParser.parse(method)
            assertTrue(result.isRight())
            result.map {
                assertEquals(SipMethodValue.valueOf(method), it.method)
            }
        }
    }

    /**
     * Test invalid method names
     */
    @Test
    fun testInvalidMethods() {
        val invalidMethods = listOf(
            "",                    // Empty method
            "invite",             // Lowercase
            "InViTe",             // Mixed case
            "UNKNOWN",            // Unknown method
            "INVITE ",            // Extra whitespace
            " INVITE",            // Leading whitespace
            "INV!TE",            // Invalid characters
            "INVITE\r\n",        // With line endings
            "METHOD-NAME"        // With hyphen
        )

        invalidMethods.forEach { method ->
            val result = methodParser.parse(method)
            assertTrue(result.isLeft())
        }
    }

    /**
     * Test parsing null or blank methods
     */
    @Test
    fun testNullOrBlankMethods() {
        val blankMethods = listOf(
            " ",
            "   ",
            "\t",
            "\n",
            "\r"
        )

        blankMethods.forEach { method ->
            val result = methodParser.parse(method)
            assertTrue(result.isLeft())
        }
    }

    /**
     * Test method serialization
     */
    @Test
    fun testMethodSerialization() {
        val method = "INVITE"
        val result = methodParser.parse(method)
        assertTrue(result.isRight())
        result.map {
            assertEquals(method, methodParser.toString(it))
        }
    }

    /**
     * Test that method parsing is case sensitive
     */
    @Test
    fun testMethodCaseSensitivity() {
        val variations = listOf(
            "Invite",
            "inVITE",
            "INVITE",
            "invite"
        )

        variations.forEach { method ->
            val result = methodParser.parse(method)
            if (method == "INVITE") {
                assertTrue(result.isRight())
            } else {
                assertTrue(result.isLeft())
            }
        }
    }

    /**
     * Test method with maximum length
     */
    @Test
    fun testMethodLength() {
        val longMethod = "A".repeat(20)
        val result = methodParser.parse(longMethod)
        assertTrue(result.isLeft())
    }
}
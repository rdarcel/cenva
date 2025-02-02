package org.daas

import io.quarkus.test.junit.QuarkusTest
import org.daas.parser.NameAddrParser
import org.daas.parser.SipUriParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import arrow.core.Some
import arrow.core.None

@QuarkusTest
class TestParserSipNameAddr {

    private val nameAddrParser = NameAddrParser(SipUriParser())

    /**
     * Test case from RFC 4475 section 3.1.1.6
     * Tests a name-addr field with no whitespace between display name and <
     */
    @Test
    fun testNoLwsBetweenDisplayNameAndBracket() {
        val result = nameAddrParser.parse("Bob<sip:bob@biloxi.com>")
        assertTrue(result.isRight(), "Should parse name-addr with no whitespace between display name and <")
        result.map {
            assertEquals(Some("Bob"), it.displayName)
            assertEquals("sip", it.uri.scheme)
            assertEquals(Some("bob"), it.uri.userInfo)
            assertEquals(Some("biloxi.com"), it.uri.host)
        }
    }

    /**
     * Test case from RFC 3261 section 20.10
     * Tests a complex name-addr field with display name containing quotes 
     */
    @Test
    fun testComplexNameAddr() {
        val result = nameAddrParser.parse("\"Alice Wilson\" <sip:alice@atlanta.com>")
        assertTrue(result.isRight(), "Should parse complex name-addr ")
        result.map {
            assertEquals(Some("Alice Wilson"), it.displayName)
            assertEquals("sip", it.uri.scheme)
            assertEquals(Some("alice"), it.uri.userInfo)
            assertEquals(Some("atlanta.com"), it.uri.host)
        }
    }

    /**
     * Test case from RFC 3261 section 20.10
     * Tests a complex name-addr field with display name containing quotes 
     */
    @Test
    fun testNoLwsBetweenDisplayNameAndQuotes() {
        val result = nameAddrParser.parse("\"Alice Wilson\"<sip:alice@atlanta.com>")
        assertTrue(result.isRight(), "Should parse complex name-with quotes and no whitespace ")
        result.map {
            assertEquals(Some("Alice Wilson"), it.displayName)
            assertEquals("sip", it.uri.scheme)
            assertEquals(Some("alice"), it.uri.userInfo)
            assertEquals(Some("atlanta.com"), it.uri.host)
        }
    }

    /**
     * Test case with only a URI (no display name)
     * Common case in SIP messages
     */
    @Test
    fun testNameAddrWithoutDisplayName() {
        val result = nameAddrParser.parse("<sip:carol@chicago.com>")
        assertTrue(result.isRight(), "Should parse name-addr without display name")
        result.map {
            assertEquals(None, it.displayName)
            assertEquals("sip", it.uri.scheme)
            assertEquals(Some("carol"), it.uri.userInfo)
            assertEquals(Some("chicago.com"), it.uri.host)
        }
    }

 
    /**
     * Test case with a display name containing special characters
     * Tests proper handling of quoted display names with special characters
     */
    @Test
    fun testNameAddrWithSpecialCharactersInDisplayName() {
        val result = nameAddrParser.parse("\"John @ Doe\" <sip:john@example.com>")
        assertTrue(result.isRight(), "Should parse name-addr with special characters in display name")
        result.map {
            assertEquals(Some("John @ Doe"), it.displayName)
            assertEquals("sip", it.uri.scheme)
            assertEquals(Some("john"), it.uri.userInfo)
            assertEquals(Some("example.com"), it.uri.host)
        }
    }

    /**
     * Test case with a display name containing escaped quotes
     */
    @Test
    fun testNameAddrWithEscaping(){
        val result = nameAddrParser.parse("\"John \\\" Doe\" <sip:john@example.com>")
        assertTrue(result.isRight(), "Should parse name-addr with escaping in display name")
        result.map { found ->
            assertEquals(Some("John \" Doe"), found.displayName)
            assertEquals("sip", found.uri.scheme)
            assertEquals(Some("john"), found.uri.userInfo)
            assertEquals(Some("example.com"), found.uri.host)
        }
    }
}
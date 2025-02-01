package org.daas

import io.quarkus.test.junit.QuarkusTest
import org.daas.parser.FromParser
import org.daas.parser.NameAddrParser
import org.daas.parser.SipUriParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import arrow.core.Some
import arrow.core.None

@QuarkusTest
class TestParserFrom {

    private val fromParser = FromParser(NameAddrParser(SipUriParser()))

    /**
     * Test a basic From header with tag parameter
     * Example from RFC 3261 Section 8.1.1.3
     */
    @Test
    fun testBasicFromWithTag() {
        val result = fromParser.parse("Alice <sip:alice@atlanta.com>;tag=1928301774")
        assertTrue(result.isRight())
        result.map { 
            assertEquals(Some("Alice"), it.address.nameAddr.displayName)
            assertEquals("sip", it.address.nameAddr.uri.scheme)
            assertEquals(Some("alice"), it.address.nameAddr.uri.userInfo)
            assertEquals(Some("atlanta.com"), it.address.nameAddr.uri.host)
            assertEquals(Some("1928301774"), it.address.tag)
        }
    }

    /**
     * Test a From header with quoted display name
     * Example from RFC 3261 Section 20.20
     */
    @Test
    fun testFromWithQuotedDisplayName() {
        val result = fromParser.parse("\"Alice Wilson\" <sip:alice@atlanta.com>;tag=9fxced76sl")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("Alice Wilson"), it.address.nameAddr.displayName)
            assertEquals(Some("9fxced76sl"), it.address.tag)
        }
    }

    /**
     * Test a From header without display name
     * Common case in SIP messages
     */
    @Test
    fun testFromWithoutDisplayName() {
        val result = fromParser.parse("<sip:bob@biloxi.com>;tag=a6c85cf")
        assertTrue(result.isRight())
        result.map {
            assertEquals(None, it.address.nameAddr.displayName)
            assertEquals(Some("a6c85cf"), it.address.tag)
        }
    }

    /**
     * Test a From header with multiple parameters
     * Including custom parameters
     */
    @Test
    fun testFromWithMultipleParameters() {
        val result = fromParser.parse("Bob <sip:bob@example.com>;tag=12345;custom=value;param")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("12345"), it.address.tag)
            assertEquals(Some("value"), it.address.parameters["custom"])
            assertEquals(None, it.address.parameters["param"])
        }
    }

    /**
     * Test a From header with SIPS URI
     * Testing secure SIP URI handling
     */
    @Test
    fun testFromWithSipsUri() {
        val result = fromParser.parse("<sips:alice@secure.example.com>;tag=ab123")
        assertTrue(result.isRight())
        result.map {
            assertEquals("sips", it.address.nameAddr.uri.scheme)
            assertEquals(Some("secure.example.com"), it.address.nameAddr.uri.host)
        }
    }

    /**
     * Test a From header with URI parameters
     * Example with transport parameter in URI
     */
    @Test
    fun testFromWithUriParameters() {
        val result = fromParser.parse("<sip:carol@chicago.com;transport=tcp>;tag=xyz123")
        assertTrue(result.isRight(), "Error with parsing From header with URI parameters, error : ${result.mapLeft { it.message }}")
        result.map {
            assertEquals(Some("tcp"), it.address.nameAddr.uri.uriParameters["transport"])
            assertEquals(Some("xyz123"), it.address.tag)
        }
    }

    /**
     * Test a From header with no tag parameter
     * Although not common, From headers without tags are valid
     */
    @Test
    fun testFromWithoutTag() {
        val result = fromParser.parse("Alice <sip:alice@wonderland.com>")
        assertTrue(result.isRight())
        result.map {
            assertEquals(None, it.address.tag)
            assertEquals(Some("alice"), it.address.nameAddr.uri.userInfo)
        }
    }

    /**
     * Test a From header with special characters in display name
     * Based on RFC 4475 examples
     */
    @Test
    fun testFromWithSpecialCharsInDisplayName() {
        val result = fromParser.parse("\"John @ Doe\" <sip:jdoe@example.com>;tag=123abc")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("John @ Doe"), it.address.nameAddr.displayName, "Error on nameaddr ${it.address.nameAddr}")
        }
    }

    /**
     * Test a From header with URI containing headers
     * Example with headers in SIP URI
     */
    @Test
    fun testFromWithUriHeaders() {
        val result = fromParser.parse("<sip:bob@biloxi.com?subject=project>;tag=99xyz")
        assertTrue(result.isRight())
        result.map {
            assertEquals("project", it.address.nameAddr.uri.headers["subject"])
        }
    }

    /**
     * Test a From header with port number in URI
     * Testing proper parsing of URI with port specification
     */
    @Test
    fun testFromWithPortNumber() {
        val result = fromParser.parse("<sip:alice@atlanta.com:5060>;tag=1234abcd")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some(5060), it.address.nameAddr.uri.port)
        }
    }

    /**
     * Test a From header with empty tag parameter
     * Testing edge case of empty parameter values
     */
    @Test
    fun testFromWithEmptyTag() {
        val result = fromParser.parse("Bob <sip:bob@example.com>;tag=")
        assertTrue(result.isRight())
        result.map {
            assertEquals(None, it.address.tag)
        }
    }

    /**
     * Test a From header with complex URI parameters
     * Testing multiple URI parameters and their values
     */
    @Test
    fun testFromWithComplexUriParameters() {
        val result = fromParser.parse("<sip:user@example.com;transport=tcp;lr;maddr=192.0.2.1>;tag=abcd")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("tcp"), it.address.nameAddr.uri.uriParameters["transport"])
            assertEquals(None, it.address.nameAddr.uri.uriParameters["lr"])
            assertEquals(Some("192.0.2.1"), it.address.nameAddr.uri.uriParameters["maddr"])
        }
    }

    /**
     * Test a From header with password in URI
     * Testing proper handling of password field in SIP URI
     */
    @Test
    fun testFromWithUriPassword() {
        val result = fromParser.parse("<sip:user:password@example.com>;tag=123")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("password"), it.address.nameAddr.uri.password)
            assertEquals(Some("user"), it.address.nameAddr.uri.userInfo)
        }
    }

    /**
     * Test a From header with multiple URI headers
     * Testing complex URI with multiple header parameters
     */
    @Test
    fun testFromWithMultipleUriHeaders() {
        val result = fromParser.parse("<sip:alice@atlanta.com?header1=value1&header2=value2>;tag=xyz")
        assertTrue(result.isRight())
        result.map {
            assertEquals("value1", it.address.nameAddr.uri.headers["header1"])
            assertEquals("value2", it.address.nameAddr.uri.headers["header2"])
        }
    }

    /**
     * Test a From header with escaped characters in display name
     * Testing proper handling of escaped characters
     */
    /**
    @Test
    fun testFromWithEscapedChars() {
        val result = fromParser.parse("\"Alice\\\"In\\\"Wonderland\" <sip:alice@example.com>;tag=123")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("Alice\"In\"Wonderland"), it.address.nameAddr.displayName)
        }
    }
        **/

    /**
     * Test a From header with very long tag parameter
     * Testing handling of long parameter values
     */
    @Test
    fun testFromWithLongTag() {
        val longTag = "a".repeat(100)
        val result = fromParser.parse("<sip:user@example.com>;tag=$longTag")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some(longTag), it.address.tag)
        }
    }

    /**
     * Test a From header with IP address as host
     * Testing URI with IP address instead of domain name
     */
    @Test
    fun testFromWithIpAddress() {
        val result = fromParser.parse("<sip:alice@192.0.2.1>;tag=abc123")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("192.0.2.1"), it.address.nameAddr.uri.host)
        }
    }

    /**
     * Test invalid From header formats
     * Testing error handling for malformed From headers
     */
    @Test
    fun testInvalidFromFormats() {
        // Missing angle brackets
        val result1 = fromParser.parse("sip:alice@atlanta.com;tag=1234")
        assertTrue(result1.isLeft())

        // Invalid tag format check if it's really the case
        val result2 = fromParser.parse("<sip:alice@atlanta.com>;tag=@#$%")
        //assertTrue(result2.isLeft())

        // Missing URI
        val result3 = fromParser.parse("Alice ;tag=1234")
        assertTrue(result3.isLeft())
    }

    /**
     * Test serialization of From header
     * Testing that parsed From headers can be correctly serialized back to string
     */
    @Test
    fun testFromSerialization() {
        val original = "\"Alice Wilson\" <sip:alice@atlanta.com>;tag=1234"
        val result = fromParser.parse(original)
        assertTrue(result.isRight())
        result.map {
            assertEquals(original, fromParser.toString(it))
        }
    }
}
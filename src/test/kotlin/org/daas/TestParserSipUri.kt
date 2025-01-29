package org.daas

import io.quarkus.test.junit.QuarkusTest
import org.daas.dao.SipParseError
import org.daas.parser.SipUriParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import arrow.core.Some
import arrow.core.None

@QuarkusTest
class TestParserSipUri {

    private val parser = SipUriParser()

    @Test
    fun testBasicSipUri() {
        val result = parser.parse("sip:alice@atlanta.com")
        assertTrue(result.isRight(),"Test a basic sip uri is parsed correctly, error : ${result.mapLeft { it.message }}")
        result.map {
            assertEquals("sip", it.scheme)
            assertEquals(Some("alice"), it.userInfo)
            assertEquals(Some("atlanta.com"), it.host)
            assertEquals(None, it.port)
        }
    }

    @Test
    fun testComplexSipUri() {
        val result = parser.parse("sip:alice:secretword@atlanta.com:5060;transport=udp?subject=project")
        assertTrue(result.isRight(), "Test that a complex sip uri is parsed correctly")
        result.map {
            assertEquals("sip", it.scheme)
            assertEquals(Some("alice"), it.userInfo)
            assertEquals(Some("secretword"), it.password)
            assertEquals(Some("atlanta.com"), it.host)
            assertEquals(Some(5060), it.port)
            assertEquals(Some("udp"), it.uriParameters["transport"])
            assertEquals("project", it.headers["subject"])
        }
    }

    /**
     * Test that a sips uri is parsed correctly
     */
    @Test
    fun testSipsUri() {
        val result = parser.parse("sips:bob@biloxi.com")
        assertTrue(result.isRight(), "Test that a sips uri is parsed correctly ${result.mapLeft { it.message }}")
        result.map {
            assertEquals("sips", it.scheme, "Test that a sips uri scheme is parsed correctly")
            assertEquals(Some("bob"), it.userInfo, "Test that the user info is parsed correctly")
            assertEquals(Some("biloxi.com"), it.host, "Test that the host is parsed correctly")
        }
    }

    /**
     * Test that a tel uri is parsed correctly
     */
    @Test
    fun testTelUri() {
        val result = parser.parse("tel:+1-201-555-0123")
        assertTrue(result.isRight(), "Test that a tel uri is parsed correctly")
        result.map {
            assertEquals("tel", it.scheme, "Test that a tel uri scheme is parsed correctly")
            assertEquals(Some("+1-201-555-0123"), it.userInfo, "Test that the user info is parsed correctly")
        }
    }

    /** 
     * Test that a tel uri with parameters is parsed correctly
     */
    @Test
    fun testTelUriWithParameters() {
        val result = parser.parse("tel:7042;phone-context=example.com")
        assertTrue(result.isRight(), "Test on tel uri with parameters works")
        result.map {
            assertEquals("tel", it.scheme, "Test on tel uri scheme parsed correctly")
            assertEquals(Some("7042"), it.userInfo, "Test on tel uri user info parsed correctly")
            assertEquals(Some("example.com"), it.phoneContext, "Test on tel uri phone context parsed correctly")
        }
    }

    /** 
     * Test that an uri with an invalid scheme fails
     */
    @Test
    fun testInvalidScheme() {
        val result = parser.parse("invalid:alice@atlanta.com")
        assertTrue(result.isLeft())
        result.mapLeft {
            assertTrue(it is SipParseError.InvalidFormat)
        }
    }

    /** 
     * Test that a uri with a missing host fails
     */
    @Test
    fun testMissingHost() {
        val result = parser.parse("sip:alice@")
   
        assertTrue(result.isLeft(),"Test on missing host fails")
    }

    /**
     * Test that a uri with a port is parsed correctly
     */
    @Test
    fun testSpecialCharactersInUserInfo() {
        val result = parser.parse("sip:alice%20smith@atlanta.com")
        assertTrue(result.isRight(),"Test that a sip uri with special characters is parsed successfully")
        result.map {
            assertEquals(Some("alice%20smith"), it.userInfo, "Test that a sip uri with special characters is parsed successfully")
        }
    }

    /**
     * Test that an uri with multiple parameters is parsed correctly
     */
    @Test
    fun testMultipleParameters() {
        val result = parser.parse("sip:alice@atlanta.com;transport=tcp;method=INVITE;ttl=3600")
        assertTrue(result.isRight(),"Test on multiple parameters works")
        result.map {
            assertEquals(Some("tcp"), it.uriParameters["transport"],"Test on multiple parameters is parsed correctly and that tcp is read correctly")
            assertEquals(Some("INVITE"), it.uriParameters["method"],"Test on multiple parameters is parsed correctly and that method is read correctly")
            assertEquals(Some("3600"), it.uriParameters["ttl"],"Test on multiple parameters is parsed correctly and that ttl is read correctly")
        }
    }

    /**
     * Test that that single header is parsed correctly is parsed correctly
     */
    @Test
    fun testSimpleHeaders() {
        val result = parser.parse("sip:alice@atlanta.com?subject=meeting")
        assertTrue(result.isRight(),"Test on multiple headers works Successfully")
        result.map {
            assertEquals("meeting", it.headers["subject"], "${result.map { it.headers.size }}")
        }
    }

    /**
     * Test that that multiple headers are parsed correctly is parsed correctly
     */
    @Test
    fun testMultipleHeaders() {
        val result = parser.parse("sip:alice@atlanta.com?subject=meeting&priority=urgent")
        assertTrue(result.isRight(),"Test on multiple headers works Successfully")
        result.map {
            assertEquals("meeting", it.headers["subject"], "${result.map { it.headers.size }}")
            assertEquals("urgent", it.headers["priority"])
        }
    }

    /**  
     * Test that a parameter without a value is parsed correctly
     */
    @Test
    fun testParameterWithoutValue() {
        val result = parser.parse("sip:alice@atlanta.com;lr")
        assertTrue(result.isRight(), "Test on parameter without value works")
        result.map {
            assertEquals(None, it.uriParameters["lr"], "Test on parameter without value is parsed correctly")
        }
    }

    /**
     * Test that an IPv4 address is parsed correctly
     */
    @Test
    fun testIPv4Address() {
        val result = parser.parse("sip:alice@192.168.1.1")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("192.168.1.1"), it.host)
        }
    }

    /**
     * Test that a tel uri with phone-context is parsed correctly
     */
    @Test
    fun testTelUriWithIsub() {
        val result = parser.parse("tel:+1-201-555-0123;isub=1234")
        assertTrue(result.isRight(),"Test on tel uri with isub works")
        result.map {
            assertEquals(Some("1234"), it.isdnSubaddress,"Test on tel uri with isub uri parameters is parsed correctly")
        }
    }

    /**
     * Test that postd uri parameters are parsed correctly
     */
    @Test
    fun testTelUriWithPostd() {
        val result = parser.parse("tel:+1-201-555-0123;postd=pp22")
        assertTrue(result.isRight(),"Test on tel uri with postd works")
        result.map {
            assertEquals(Some("pp22"), it.postDial,"Test on tel uri with postd uri parameters is parsed correctly")
        }
    }

    /**
     * Test that an empty uri fails to be parsed
     */
    @Test
    fun testEmptyUri() {
        val result = parser.parse("")
        assertTrue(result.isLeft(),"Test on empty uri fails")
    }

    /**
     * Test that a malformed port fails if provided
     */
    @Test
    fun testMalformedPort() {
        val result = parser.parse("sip:alice@atlanta.com:abcd")
        assertTrue(result.isLeft(),"Test that a malformed port fails")
    }

    @Test
    fun testUriToString() {
        val uri = "sip:alice@atlanta.com:5060;transport=udp?subject=meeting"
        val result = parser.parse(uri)
        assertTrue(result.isRight())
        result.map {
            assertEquals(uri, parser.toString(it),"Test that parsing then regenerating a sip uri works")
        }
    }

    /** 
     * Test that a tel uri can be converted to a string identical to the parsed element
     */
    @Test
    fun testTelUriToString() {
        val uri = "tel:+1-201-555-0123;phone-context=example.com"
        val result = parser.parse(uri)
        assertTrue(result.isRight())
        result.map {
            assertEquals(uri, parser.toString(it), "Test that parsing then regenerating  a tel uri works")
        }
    }

    /**
     *  Test that an uri with invalid characters in the user info fails
     */
    @Test
    fun testInvalidUserInfoCharacters() {
        val result = parser.parse("sip:alice[]@atlanta.com")
        assertTrue(result.isLeft(),"Test on invalid user info name fails")
    }

    /**
     * Test that a uri with spaces fails
     */
    @Test
    fun testUriWithSpaces() {
        val result = parser.parse("sip: alice@atlanta.com")
        assertTrue(result.isLeft(), "Test on spaces in uri fails")
    }

    /**
     * Test the case of a request uri (with a user info empty)
     */
    @Test
    fun testRequestUri(){
        val result = parser.parse("sip:chicago.com")
        assertTrue(result.isRight(), "Test on request uri works")
        result.map {
            assertEquals("sip", it.scheme, "Test on request uri scheme is parsed correctly")
            assertEquals(None, it.userInfo, "Test on request uri user info is parsed correctly (none)")
            assertEquals(Some("chicago.com"), it.host, "Test on request uri host is parsed correctly")
        }
    }
}
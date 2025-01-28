package org.daas

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.daas.parser.SipParserUri
import org.daas.dao.SipUri


@QuarkusTest
class TestParserSipUri {

    @Test
    fun testBasicSipUri() {
        val uri = "sip:alice@example.com"
        val parser = SipParserUri().parse(uri)
        parser.fold(
            { assertTrue(false, "Parsing error : ${it.message}")  },
            { 
                assertEquals("alice", it.userInfo)
                assertEquals("example.com", it.host)
            }
        )
  
        assertEquals("alice", parser.user)
        assertEquals("example.com", parser.host)
        assertNull(parser.port)
    }

    @Test
    fun testSipUriWithPort() {
        val uri = "sip:bob@example.com:5060"
        val parser = SipParserUri().parse(uri)
        assertEquals("bob", parser.user)
        assertEquals("example.com", parser.host)
        assertEquals(5060, parser.port)
    }

    @Test
    fun testSipUriWithParameters() {
        val uri = "sip:carol@example.com;transport=tcp"
        val parser = SipParserUri().parse(uri)
        assertEquals("carol", parser.user)
        assertEquals("example.com", parser.host)
        assertEquals("tcp", parser.parameters["transport"])
    }

    @Test
    fun testSipsUri() {
        val uri = "sips:dave@secure.com"
        val parser = SipParserUri().parse(uri)
        assertTrue(parser.isSecure)
        assertEquals("dave", parser.user)
        assertEquals("secure.com", parser.host)
    }

    @Test
    fun testComplexSipUri() {
        val uri = "sip:alice:password@example.com:5060;transport=tcp?subject=meeting"
        val parser = SipParserUri().parse(uri)
        assertEquals("alice", parser.user)
        assertEquals("password", parser.password)
        assertEquals("example.com", parser.host)
        assertEquals(5060, parser.port)
        assertEquals("tcp", parser.parameters["transport"])
        assertEquals("meeting", parser.headers["subject"])
    }

    @Test
    fun testSipUriWithMultipleParameters() {
        val uri = "sip:bob@example.com;transport=udp;ttl=5;method=INVITE"
        val parser = SipParserUri().parse(uri)
        assertEquals("bob", parser.user)
        assertEquals("example.com", parser.host)
        assertEquals("udp", parser.parameters["transport"])
        assertEquals("5", parser.parameters["ttl"])
        assertEquals("INVITE", parser.parameters["method"])
    }

    @Test
    fun testSipUriWithHeaders() {
        val uri = "sip:carol@example.com?subject=call&priority=urgent"
        val parser = SipParserUri().parse(uri)
        assertEquals("carol", parser.user)
        assertEquals("example.com", parser.host)
        assertEquals("call", parser.headers["subject"])
        assertEquals("urgent", parser.headers["priority"])
    }

    @Test
    fun testSipUriWithIPv4() {
        val uri = "sip:alice@192.168.1.1"
        val parser = SipParserUri().parse(uri)
        assertEquals("alice", parser.user)
        assertEquals("192.168.1.1", parser.host)
    }

    @Test
    fun testSipUriWithIPv6() {
        val uri = "sip:bob@[2001:db8::1]:5060"
        val parser = SipParserUri().parse(uri)
        assertEquals("bob", parser.user)
        assertEquals("[2001:db8::1]", parser.host)
        assertEquals(5060, parser.port)
    }

    @Test
    fun testSipUriWithoutUser() {
        val uri = "sip:example.com"
        val parser = SipParserUri().parse(uri)
        assertNull(parser.user)
        assertEquals("example.com", parser.host)
    }
}
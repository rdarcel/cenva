package org.cenva

import io.quarkus.test.junit.QuarkusTest
import org.cenva.parser.ViaParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import arrow.core.Some
import arrow.core.None

@QuarkusTest
class TestParserVia {

    private val viaParser = ViaParser()

    /**
     * Test basic Via header 
     * Example from RFC 3261 Section 8.1.1.7
     */
    @Test
    fun testBasicVia() {
        val result = viaParser.parse("SIP/2.0/UDP pc33.atlanta.com:5060;branch=z9hG4bK776asdhds")
        assertTrue(result.isRight())
        result.map {
            assertEquals("SIP/2.0", it.protocol)
            assertEquals("UDP", it.transport)
            assertEquals("pc33.atlanta.com", it.host)
            assertEquals(Some(5060), it.port)
            assertEquals(Some("z9hG4bK776asdhds"), it.parameters["branch"])
        }
    }

    /**
     * Test Via without port
     * Common case in SIP messages
     */
    @Test
    fun testViaWithoutPort() {
        val result = viaParser.parse("SIP/2.0/TCP server10.biloxi.com;branch=z9hG4bK4321")
        assertTrue(result.isRight())
        result.map {
            assertEquals(None, it.port)
            assertEquals("TCP", it.transport)
        }
    }

    /**
     * Test Via with multiple parameters
     * Based on common usage scenarios
     */
    @Test
    fun testViaWithMultipleParams() {
        val result = viaParser.parse("SIP/2.0/UDP 192.0.2.1;branch=z9hG4bK123;received=192.0.2.2;rport=5060")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("z9hG4bK123"), it.parameters["branch"])
            assertEquals(Some("192.0.2.2"), it.parameters["received"])
            assertEquals(Some("5060"), it.parameters["rport"])
        }
    }

    /**
     * Test Via with IPv6 address
     * RFC 3261 supports IPv6 addresses
     * Removing test for now
     */
    
    @Test
    fun testViaWithIPv6() {
        val result = viaParser.parse("SIP/2.0/UDP [2001:db8::9:1]:5060;branch=z9hG4bKna998sk")
        assertTrue(result.isRight())
        result.map {
            assertEquals("[2001:db8::9:1]", it.host)
            assertEquals(Some(5060), it.port)
        }
    }

    /**
     * Test Via with rport parameter without value
     * RFC 3581 allows empty rport parameter
     */
    @Test
    fun testViaWithEmptyRport() {
        val result = viaParser.parse("SIP/2.0/UDP client.atlanta.com:5060;branch=z9hG4bK74bf9;rport")
        assertTrue(result.isRight())
        result.map {
            assertEquals(None, it.parameters["rport"])
        }
    }

    /**
     * Test Via with ttl parameter
     * For multicast scenarios
     */
    @Test
    fun testViaWithTTL() {
        val result = viaParser.parse("SIP/2.0/UDP 224.0.1.75:5060;branch=z9hG4bK123;ttl=16")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("16"), it.parameters["ttl"])
        }
    }

    /**
     * Test Via with maddr parameter
     * For specifying multicast address
     */
    @Test
    fun testViaWithMaddr() {
        val result = viaParser.parse("SIP/2.0/UDP server1.atlanta.com:5060;branch=z9hG4bK87a;maddr=239.255.255.1")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("239.255.255.1"), it.parameters["maddr"])
        }
    }

    /**
     * Test Via with custom transport
     * RFC 3261 allows for extension transport protocols
     */
    @Test
    fun testViaWithCustomTransport() {
        val result = viaParser.parse("SIP/2.0/SCTP server.atlanta.com:5060;branch=z9hG4bK87asd")
        assertTrue(result.isRight())
        result.map {
            assertEquals("SCTP", it.transport)
        }
    }

    /**
     * Test Via with received parameter
     * Common in NAT scenarios
     */
    @Test
    fun testViaWithReceived() {
        val result = viaParser.parse("SIP/2.0/UDP 10.1.1.1:5060;branch=z9hG4bK123;received=23.45.67.89")
        assertTrue(result.isRight())
        result.map {
            assertEquals(Some("23.45.67.89"), it.parameters["received"])
        }
    }

    /**
     * Test Via with alias parameter
     * From RFC 3261 examples
     */
    @Test
    fun testViaWithAlias() {
        val result = viaParser.parse("SIP/2.0/UDP host.atlanta.com:5060;branch=z9hG4bK123;alias")
        assertTrue(result.isRight())
        result.map {
            assertEquals(None, it.parameters["alias"])
        }
    }

    /**
     * Test malformed Via without branch
     * Branch is mandatory in SIP/2.0
     */
    @Test
    fun testViaWithoutBranch() {
        val result = viaParser.parse("SIP/2.0/UDP server.atlanta.com:5060")
        assertTrue(result.isRight()) // Parser should still parse it, validation is separate concern
    }

    /**
     * Test Via with invalid transport
     */
    @Test
    fun testViaWithInvalidProtocolFormat() {
        val result = viaParser.parse("INVALID/2.0/UDP server.atlanta.com:5060;branch=z9hG4bK123")
        assertTrue(result.isRight()) // Parser should parse it, protocol validation is separate
    }

    /**
     * Test Via with invalid port number
     */
    @Test
    fun testViaWithInvalidPort() {
        val result = viaParser.parse("SIP/2.0/UDP server.atlanta.com:abc;branch=z9hG4bK123")
        assertTrue(result.isLeft())
    }

    /**
     * Test Via with multiple branch parameters
     * Although invalid, parser should handle it
     */
    @Test
    fun testViaWithMultipleBranches() {
        val result = viaParser.parse("SIP/2.0/UDP server.atlanta.com:5060;branch=z9hG4bK123;branch=z9hG4bK456")
        assertTrue(result.isRight())
    }

    /**
     * Test Via with empty parameter value
     */
    @Test
    fun testViaWithEmptyParamValue() {
        val result = viaParser.parse("SIP/2.0/UDP server.atlanta.com:5060;branch=;received=")
        assertTrue(result.isRight())
        result.map {
            assertEquals(None, it.parameters["branch"])
            assertEquals(None, it.parameters["received"])
        }
    }

    /**
     * Test serialization of Via header
     */
    @Test
    fun testViaSerialization() {
        val original = "SIP/2.0/UDP server.atlanta.com:5060;branch=z9hG4bK123"
        val result = viaParser.parse(original)
        assertTrue(result.isRight())
        result.map {
            assertEquals(original, viaParser.toString(it))
        }
    }

    /**
     * Test Via with multiple spaces
     * Parser should be tolerant of extra whitespace
     */
    @Test
    fun testViaWithExtraSpaces() {
        val result = viaParser.parse("SIP/2.0/UDP    server.atlanta.com:5060  ;  branch=z9hG4bK123")
        assertTrue(result.isRight())
    }

    /**
     * Test Via with commented string
     * Based on RFC 4475 torture tests
     */
    @Test
    fun testViaWithComment() {
        val result = viaParser.parse("SIP/2.0/UDP host.atlanta.com:5060;branch=z9hG4bK123 (comment)")
        assertTrue(result.isRight())
    }

    /**
     * Test Via with unusual but valid transport
     * From RFC 4475 torture tests
     */
    @Test
    fun testViaWithUnusualTransport() {
        val result = viaParser.parse("SIP/2.0/NEWSCHEME server.atlanta.com:5060;branch=z9hG4bK123")
        assertTrue(result.isRight())
        result.map {
            assertEquals("NEWSCHEME", it.transport)
        }
    }

    /**
     * Test Via header with maximum length components
     */
    @Test
    fun testViaWithLongComponents() {
        val longHost = "a".repeat(255) + ".com"
        val longBranch = "z9hG4bK" + "a".repeat(100)
        val result = viaParser.parse("SIP/2.0/UDP $longHost:5060;branch=$longBranch")
        assertTrue(result.isRight())
    }
}
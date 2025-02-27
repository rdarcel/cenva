package org.daas

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.cenva.dao.sip.*
import org.cenva.parser.SipParserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class TesterParserSimpleHeaders {

    @Inject
    lateinit var sipParserService: SipParserService

    /**
     * Test 1: Verify that a SIP request with an Expires header is correctly parsed.
     * This test checks that the parser can extract a simple numeric value from the Expires header.
     */
    @Test
    fun testParseExpiresHeader() {
        val message = """
            REGISTER sip:registrar.biloxi.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Contact: <sip:bob@192.0.2.4>
            Expires: 3600
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with Expires header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.expires, "Expires header should be present in the parsed message")
            assertEquals(3600, sipMsg.expires.orNull(), "Expires value should be 3600 seconds")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Expires: 3600"), 
                "Serialized message should contain the Expires header with value 3600")
        }
    }

    /**
     * Test 2: Verify that a SIP response with an Expires header in a short format is correctly parsed.
     * This test checks that the parser can handle a different context for the Expires header.
     */
    @Test
    fun testParseExpiresInResponse() {
        val message = """
            SIP/2.0 200 OK
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKnashds8
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>;tag=1410948204
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Contact: <sip:bob@192.0.2.4>
            Expires: 1800
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP response with Expires header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipResponse, "Parsed message should be a SipResponse")
            assertNotNull(sipMsg.expires, "Expires header should be present in the parsed message")
            assertEquals(1800, sipMsg.expires.orNull(), "Expires value should be 1800 seconds")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Expires: 1800"), 
                "Serialized message should contain the Expires header with value 1800")
        }
    }

    /**
     * Test 3: Verify that a SIP request with a Min-Expires header is correctly parsed.
     * This test checks that the parser can extract a simple numeric value from the Min-Expires header.
     */
    @Test
    fun testParseMinExpiresHeader() {
        val message = """
            REGISTER sip:registrar.biloxi.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Contact: <sip:bob@192.0.2.4>
            Min-Expires: 1200
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with Min-Expires header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.minExpires, "Min-Expires header should be present in the parsed message")
            assertEquals(1200, sipMsg.minExpires.orNull(), "Min-Expires value should be 1200 seconds")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Min-Expires: 1200"), 
                "Serialized message should contain the Min-Expires header with value 1200")
        }
    }

    /**
     * Test 4: Verify that a SIP response with a Min-Expires header is correctly parsed,
     * particularly in a 423 Interval Too Brief response where this header is required.
     */
    @Test
    fun testParseMinExpiresInResponse() {
        val message = """
            SIP/2.0 423 Interval Too Brief
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKnashds8
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>;tag=1410948204
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Min-Expires: 3600
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP response with Min-Expires header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipResponse, "Parsed message should be a SipResponse")
            assertNotNull(sipMsg.minExpires, "Min-Expires header should be present in the parsed message")
            assertEquals(3600, sipMsg.minExpires.orNull(), "Min-Expires value should be 3600 seconds")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Min-Expires: 3600"), 
                "Serialized message should contain the Min-Expires header with value 3600")
        }
    }

    /**
     * Test 5: Verify that a SIP request with a Supported header containing a single option token
     * is correctly parsed.
     */
    @Test
    fun testParseSupportedHeaderSingleOption() {
        val message = """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Alice <sip:alice@atlanta.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 INVITE
            Contact: <sip:bob@192.0.2.4>
            Supported: timer
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with Supported header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.supported, "Supported header should be present in the parsed message")
            
            val supported = sipMsg.supported.orNull()
            assertNotNull(supported, "Supported value should not be null")
            assertEquals(1, supported?.size, "There should be exactly one supported option")
            assertEquals("timer", supported?.get(0), "The supported option should be 'timer'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Supported: timer"), 
                "Serialized message should contain the Supported header with value 'timer'")
        }
    }

    /**
     * Test 6: Verify that a SIP request with a Supported header containing multiple option tokens
     * is correctly parsed. This tests the parser's ability to handle comma-separated values in the header.
     */
    @Test
    fun testParseSupportedHeaderMultipleOptions() {
        val message = """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Alice <sip:alice@atlanta.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 INVITE
            Contact: <sip:bob@192.0.2.4>
            Supported: timer, 100rel, path, replaces
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with multiple Supported options should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.supported, "Supported header should be present in the parsed message")
            
            val supported = sipMsg.supported.orNull()
            assertNotNull(supported, "Supported value should not be null")
            assertEquals(4, supported?.size, "There should be exactly four supported options")
            assertTrue(supported?.contains("timer") == true, "The supported options should include 'timer'")
            assertTrue(supported?.contains("100rel") == true, "The supported options should include '100rel'")
            assertTrue(supported?.contains("path") == true, "The supported options should include 'path'")
            assertTrue(supported?.contains("replaces") == true, "The supported options should include 'replaces'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Supported:"), 
                "Serialized message should contain the Supported header")
            // Check for all supported values in the serialized output
            supported?.forEach { option ->
                assertTrue(serialized.contains(option), 
                    "Serialized message should contain the supported option '$option'")
            }
        }
    }

    /**
     * Test 7: Verify that a SIP request with an Allow header containing a single method
     * is correctly parsed.
     */
    @Test
    fun testParseAllowHeaderSingleMethod() {
        val message = """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Alice <sip:alice@atlanta.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 INVITE
            Contact: <sip:bob@192.0.2.4>
            Allow: INVITE
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with Allow header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.allow, "Allow header should be present in the parsed message")
            
            val allow = sipMsg.allow.orNull()
            assertNotNull(allow, "Allow value should not be null")
            assertEquals(1, allow?.size, "There should be exactly one allowed method")
            assertEquals("INVITE", allow?.get(0), "The allowed method should be 'INVITE'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Allow: INVITE"), 
                "Serialized message should contain the Allow header with value 'INVITE'")
        }
    }

    /**
     * Test 8: Verify that a SIP request with an Allow header containing multiple methods
     * is correctly parsed. This tests the parser's ability to handle comma-separated methods.
     */
    @Test
    fun testParseAllowHeaderMultipleMethods() {
        val message = """
            OPTIONS sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Alice <sip:alice@atlanta.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 OPTIONS
            Contact: <sip:bob@192.0.2.4>
            Allow: INVITE, ACK, CANCEL, BYE, OPTIONS
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with multiple Allow methods should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.allow, "Allow header should be present in the parsed message")
            
            val allow = sipMsg.allow.orNull()
            assertNotNull(allow, "Allow value should not be null")
            assertEquals(5, allow?.size, "There should be exactly five allowed methods")
            assertTrue(allow?.contains("INVITE") == true, "The allowed methods should include 'INVITE'")
            assertTrue(allow?.contains("ACK") == true, "The allowed methods should include 'ACK'")
            assertTrue(allow?.contains("CANCEL") == true, "The allowed methods should include 'CANCEL'")
            assertTrue(allow?.contains("BYE") == true, "The allowed methods should include 'BYE'")
            assertTrue(allow?.contains("OPTIONS") == true, "The allowed methods should include 'OPTIONS'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Allow:"), "Serialized message should contain the Allow header")
            // Check for all allowed methods in the serialized output
            allow?.forEach { method ->
                assertTrue(serialized.contains(method), 
                    "Serialized message should contain the allowed method '$method'")
            }
        }
    }

    /**
     * Test 9: Verify that a SIP request with a User-Agent header containing simple information
     * is correctly parsed.
     */
    @Test
    fun testParseUserAgentHeaderSimple() {
        val message = """
            REGISTER sip:registrar.biloxi.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Contact: <sip:bob@192.0.2.4>
            User-Agent: SIPPhone
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with User-Agent header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.userAgent, "User-Agent header should be present in the parsed message")
            assertEquals("SIPPhone", sipMsg.userAgent.orNull(), "User-Agent value should be 'SIPPhone'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("User-Agent: SIPPhone"), 
                "Serialized message should contain the User-Agent header with value 'SIPPhone'")
        }
    }

    /**
     * Test 10: Verify that a SIP request with a User-Agent header containing complex information
     * including product name and version is correctly parsed.
     */
    @Test
    fun testParseUserAgentHeaderComplex() {
        val message = """
            REGISTER sip:registrar.biloxi.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Contact: <sip:bob@192.0.2.4>
            User-Agent: SoftClient v2.0.1 (Windows NT 10.0; Win64; x64)
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with complex User-Agent header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.userAgent, "User-Agent header should be present in the parsed message")
            val expectedUserAgent = "SoftClient v2.0.1 (Windows NT 10.0; Win64; x64)"
            assertEquals(expectedUserAgent, sipMsg.userAgent.orNull(), 
                "User-Agent value should match the complex string")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("User-Agent: $expectedUserAgent"), 
                "Serialized message should contain the complex User-Agent header value")
        }
    }

    /**
     * Test 11: Verify that a SIP request with a Require header containing a single extension
     * is correctly parsed.
     */
    @Test
    fun testParseRequireHeaderSingleExtension() {
        val message = """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Alice <sip:alice@atlanta.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 INVITE
            Contact: <sip:bob@192.0.2.4>
            Require: 100rel
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with Require header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.require, "Require header should be present in the parsed message")
            
            val require = sipMsg.require.orNull()
            assertNotNull(require, "Require value should not be null")
            assertEquals(1, require?.size, "There should be exactly one required extension")
            assertEquals("100rel", require?.get(0), "The required extension should be '100rel'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Require: 100rel"), 
                "Serialized message should contain the Require header with value '100rel'")
        }
    }

    /**
     * Test 12: Verify that a SIP request with a Require header containing multiple extensions
     * is correctly parsed. This tests the parser's ability to handle comma-separated values.
     */
    @Test
    fun testParseRequireHeaderMultipleExtensions() {
        val message = """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP pc33.atlanta.com;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Alice <sip:alice@atlanta.com>
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 INVITE
            Contact: <sip:bob@192.0.2.4>
            Require: 100rel, precondition, timer
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP message with multiple Require extensions should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipRequest, "Parsed message should be a SipRequest")
            assertNotNull(sipMsg.require, "Require header should be present in the parsed message")
            
            val require = sipMsg.require.orNull()
            assertNotNull(require, "Require value should not be null")
            assertEquals(3, require?.size, "There should be exactly three required extensions")
            assertTrue(require?.contains("100rel") == true, "The required extensions should include '100rel'")
            assertTrue(require?.contains("precondition") == true, "The required extensions should include 'precondition'")
            assertTrue(require?.contains("timer") == true, "The required extensions should include 'timer'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Require:"), "Serialized message should contain the Require header")
            // Check for all required extensions in the serialized output
            require?.forEach { extension ->
                assertTrue(serialized.contains(extension), 
                    "Serialized message should contain the required extension '$extension'")
            }
        }
    }

    /**
     * Test 13: Verify that a SIP response with a Server header containing simple information
     * is correctly parsed.
     */
    @Test
    fun testParseServerHeaderSimple() {
        val message = """
            SIP/2.0 200 OK
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKnashds8
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>;tag=1410948204
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Contact: <sip:bob@192.0.2.4>
            Server: SIPServer
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP response with Server header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipResponse, "Parsed message should be a SipResponse")
            assertNotNull(sipMsg.server, "Server header should be present in the parsed message")
            assertEquals("SIPServer", sipMsg.server.orNull(), "Server value should be 'SIPServer'")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Server: SIPServer"), 
                "Serialized message should contain the Server header with value 'SIPServer'")
        }
    }

    /**
     * Test 14: Verify that a SIP response with a Server header containing detailed product
     * and version information is correctly parsed.
     */
    @Test
    fun testParseServerHeaderDetailed() {
        val message = """
            SIP/2.0 200 OK
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKnashds8
            From: Bob <sip:bob@biloxi.com>;tag=a73kszlfl
            To: Bob <sip:bob@biloxi.com>;tag=1410948204
            Call-ID: 1j9FpLxk3uxtm8tn@biloxi.com
            CSeq: 1 REGISTER
            Contact: <sip:bob@192.0.2.4>
            Server: SIP Server 3.0 (CentOS 7.9; Linux x86_64)
            
        """.trimIndent()

        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "The SIP response with detailed Server header should be parsed successfully")
        
        result.map { sipMsg ->
            assertTrue(sipMsg is SipResponse, "Parsed message should be a SipResponse")
            assertNotNull(sipMsg.server, "Server header should be present in the parsed message")
            val expectedServer = "SIP Server 3.0 (CentOS 7.9; Linux x86_64)"
            assertEquals(expectedServer, sipMsg.server.orNull(), 
                "Server value should match the detailed string")
            
            // Test serialization
            val serialized = sipParserService.toString(sipMsg)
            assertTrue(serialized.contains("Server: $expectedServer"), 
                "Serialized message should contain the detailed Server header value")
        }
    }

}
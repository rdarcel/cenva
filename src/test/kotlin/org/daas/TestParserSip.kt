package org.acme

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.daas.parser.SipParserService
import org.daas.dao.SipRequest
import org.daas.dao.SipResponse
import org.daas.dao.SipParseError
import jakarta.inject.Inject



@QuarkusTest
class TestParserSip {

    @Inject
    private lateinit var sipParser: SipParserService

    @Test
    fun testSipRequestParsing() {
        // Test REGISTER request
        val registerRequest = """
            REGISTER sip:registrar.example.com SIP/2.0
            Via: SIP/2.0/UDP pc33.example.com:5060;branch=z9hG4bKnashds8
            Max-Forwards: 70
            To: Bob <sip:bob@example.com>
            From: Alice <sip:alice@example.com>;tag=1928301774
            Call-ID: a84b4c76e66710@pc33.example.com
            CSeq: 314159 REGISTER
            Contact: <sip:alice@pc33.example.com>
            Content-Length: 0
        """.trimIndent()
        
        val parsedRegister = sipParser.parse(registerRequest)
        parsedRegister.fold(
            { assertTrue(false, "Parsing error : ${it.message}")  },
            { 
                when(it) {
                    is SipRequest -> {
                        assertEquals("REGISTER", it.method)
                        assertEquals("2.0", it.sipVersion)
                        assertEquals("sip:registrar.example.com", it.requestUri)

                        // Test that serialization works correctly
                        //assertEquals(registerRequest, it.toString())
                       
                    }
                    else -> assertTrue(false)
                }          
            }
        )
      
    
      
        


        // Test INVITE request
        val inviteRequest = """
            INVITE sip:bob@example.com SIP/2.0
            Via: SIP/2.0/UDP pc33.example.com:5060;branch=z9hG4bK776asdhds
            Max-Forwards: 70
            To: Bob <sip:bob@example.com>
            From: Alice <sip:alice@example.com>;tag=1928301774
            Call-ID: a84b4c76e66710@pc33.example.com
            CSeq: 314159 INVITE
            Contact: <sip:alice@pc33.example.com>
            Content-Type: application/sdp
            Content-Length: 142

            v=0
            o=alice 2890844526 2890844526 IN IP4 pc33.example.com
            s=-
            c=IN IP4 pc33.example.com
            t=0 0
            m=audio 49172 RTP/AVP 0
            a=rtpmap:0 PCMU/8000
        """.trimIndent()
        
        val parsedInvite = sipParser.parse(inviteRequest)
        parsedInvite.fold(
            { assertTrue(false, "Parsing error : ${it.message}")  },
            { 
                when(it) {
                    is SipRequest -> {
                        assertEquals("INVITE", it.method)
                        assertEquals("2.0", it.sipVersion)
                        assertEquals("sip:bob@example.com", it.requestUri)
                       
                    }
                    else -> assertTrue(false)
                }          
            }
        )
   
    }

    @Test
    fun testSipResponseParsing() {
        // Test 200 OK response
        val okResponse = """
            SIP/2.0 200 OK
            Via: SIP/2.0/UDP server10.example.com;branch=z9hG4bK4442ba5c
            Via: SIP/2.0/UDP pc33.example.com;branch=z9hG4bK776asdhds
            To: Bob <sip:bob@example.com>;tag=2493k59kd
            From: Alice <sip:alice@example.com>;tag=1928301774
            Call-ID: a84b4c76e66710@pc33.example.com
            CSeq: 314159 INVITE
            Contact: <sip:bob@pc33.example.com>
            Content-Length: 0
        """.trimIndent()
        
        val parsedResponse = sipParser.parse(okResponse)
        parsedResponse.fold( 
            { assertTrue(false,"Parsing error ${it.message}")  },
            { 
                when(it) {
                    is SipResponse -> {
                        assertEquals(200, it.statusCode)
                        assertEquals("OK", it.reasonPhrase)
                    }
                    else -> assertTrue(false)
                }          
            }
        )

    }

    @Test
    fun testErrorCases() {
        // Test invalid SIP version
        val invalidVersionRequest = """
            REGISTER sip:registrar.example.com SIP/1.0
            Via: SIP/2.0/UDP pc33.example.com:5060
            Content-Length: 0
        """.trimIndent()

        val errorInvalidVersion = sipParser.parse(invalidVersionRequest)
        errorInvalidVersion.fold(
            { assertTrue(true, "Error returned for Invalid SIP version : SIP/1.0") },
            { assertTrue(false,"Should return error : Invalid SIP version : SIP/1.0") }
        )


        // Test unsupported method
        val unsupportedMethodRequest = """
            UNKNOWN sip:registrar.example.com SIP/2.0
            Via: SIP/2.0/UDP pc33.example.com:5060
            Content-Length: 0
        """.trimIndent()
        
        val errorUnsupportedMethod = sipParser.parse(unsupportedMethodRequest)
        errorUnsupportedMethod.fold(
            { assertTrue(true, "Error returned for Unsupported method : UNKNOWN") },
            { assertTrue(false,"Should return error : Unsupported method : UNKNOWN") }
        )
         
      
    }

    @Test
    fun testSpecialCases() {
        // Test request with multiple Via headers
        val multiViaRequest = """
            INVITE sip:bob@example.com SIP/2.0
            Via: SIP/2.0/UDP proxy1.example.com;branch=z9hG4bK123
            Via: SIP/2.0/UDP pc33.example.com;branch=z9hG4bK776asdhds
            To: Bob <sip:bob@example.com>
            From: Alice <sip:alice@example.com>;tag=1928301774
            Call-ID: multi-via@pc33.example.com
            CSeq: 1 INVITE
            Content-Length: 0
        """.trimIndent()
        
        val parsedMultiVia = sipParser.parse(multiViaRequest)
        parsedMultiVia.fold(
            { assertTrue(false,"Parsing error : ${it.message}")  },
            { 
                when(it) {
                    is SipRequest -> {        
                        assertEquals(2, it.headers["Via"]?.size)
                    }
                    else -> assertTrue(false)
                }          
            }
        )
     

        // Test request with custom headers
        val customHeaderRequest = """
            MESSAGE sip:bob@example.com SIP/2.0
            Via: SIP/2.0/UDP pc33.example.com;branch=z9hG4bK776asdhds
            To: Bob <sip:bob@example.com>
            From: Alice <sip:alice@example.com>;tag=1928301774
            Call-ID: custom@pc33.example.com
            CSeq: 1 MESSAGE
            X-Custom-Header: custom value
            Content-Type: text/plain
            Content-Length: 13

            Hello, World!
        """.trimIndent()
        
        val parsedCustom = sipParser.parse(customHeaderRequest)
        parsedCustom.fold(
            { assertTrue(false, "Parsing error : ${it.message}")  },
            { 
                when(it) {
                    is SipRequest -> { 
                        assertEquals("custom value", it.headers["X-Custom-Header"]?.get(0))
                    }
                    else -> assertTrue(false)
                }          
            }
        )
    }
        
 }

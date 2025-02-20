package org.daas

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.cenva.dao.sip.*
import org.cenva.parser.SipParserService
import org.cenva.parser.SipUriParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class TestSipParserTest {

    @Inject lateinit var sipParserService: SipParserService

    private val sipUriParser = SipUriParser(false)

    /**
     * Test 1: Verify that a valid INVITE request is parsed correctly. Checks method, URI, SIP
     * version and required headers.
     */
    @Test
    fun testValidInviteRequest() {
        val message =
                """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKnashds8
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=123456
            To: Alice <sip:alice@atlanta.com>
            Call-ID: abcdefgh@biloxi.com
            CSeq: 1 INVITE

            v=0
            o=Alice 2890844526 2890844526 IN IP4 host.atlanta.com
            s=-
            c=IN IP4 host.atlanta.com
            t=0 0
            m=audio 49170 RTP/AVP 0
        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "Expected the INVITE message to be parsed successfully")
        result.map { sipMsg ->
            // Verify that the parsed message is a SipRequest
            assertTrue(sipMsg is SipRequest, "Parsed message must be a SipRequest")
            sipMsg as SipRequest
            // Check the SIP method
            assertEquals(SipMethodValue.INVITE, sipMsg.method.method, "Method should be INVITE")
            // Check the request URI string representation
            assertEquals(
                    "sip:alice@atlanta.com",
                    sipUriParser.toString(sipMsg.uri),
                    "URI must match the expected value"
            )
            // Check the SIP version
            assertEquals(
                    SipVersionValue.SIP_2_0,
                    sipMsg.sipVersion.version,
                    "SIP version should be 2.0"
            )
            // Check for existence of Call-ID header
            assertNotNull(sipMsg.callId, "Call-ID header must be present")
            // Check for existence of To header
            assertNotNull(sipMsg.to, "To header must be present")
        }
    }

    /**
     * Test 2: Verify that a valid BYE request is parsed correctly. Checks that the method is BYE
     * and the CSeq header is correct.
     */
    @Test
    fun testValidByeRequest() {
        val message =
                """
            BYE sip:bob@biloxi.com SIP/2.0
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKdfge
            Max-Forwards: 70
            From: Alice <sip:alice@atlanta.com>;tag=654321
            To: Bob <sip:bob@biloxi.com>;tag=abcdef
            Call-ID: zyxwvuts@atlanta.com
            CSeq: 2 BYE

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Expected the BYE message to be parsed successfully " +
                        result.mapLeft { it.message }
        )
        result.map { sipMsg ->
            // Ensure the parsed message is a SipRequest
            assertTrue(sipMsg is SipRequest, "Parsed message must be a SipRequest")
            sipMsg as SipRequest
            // Validate the SIP method
            assertEquals(SipMethodValue.BYE, sipMsg.method.method, "Method should be BYE")
            // Validate the CSeq sequence number
            assertEquals(2L, sipMsg.cSeq.sequenceNumber, "CSeq sequence number must be 2")
        }
    }

    /**
     * Test 3: Verify that a valid REGISTER request is parsed correctly. Checks that From, To,
     * Call-ID and CSeq headers are properly processed.
     */
    @Test
    fun testValidRegisterRequest() {
        val message =
                """
            REGISTER sip:registrar.biloxi.com SIP/2.0
            Via: SIP/2.0/UDP client.biloxi.com;branch=z9hG4bK849
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=112233
            To: Bob <sip:bob@biloxi.com>
            Call-ID: xyz123@client.biloxi.com
            CSeq: 3 REGISTER

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Expected the REGISTER message to be parsed successfully" +
                        result.mapLeft { it.message }
        )
        result.map { sipMsg ->
            // Ensure the parsed message is a SipRequest
            assertTrue(sipMsg is SipRequest, "Parsed message must be a SipRequest")
            sipMsg as SipRequest
            // Validate the SIP method
            assertEquals(SipMethodValue.REGISTER, sipMsg.method.method, "Method should be REGISTER")
            // Validate the CSeq sequence number
            assertEquals(3L, sipMsg.cSeq.sequenceNumber, "CSeq sequence number must be 3")
        }
    }

    /**
     * Test 4: Verify that a valid SIP response with status 200 OK is parsed correctly. Checks SIP
     * version, status code and reason phrase.
     */
    @Test
    fun testValidResponseOk() {
        val message =
                """
            SIP/2.0 200 OK
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bK12345
            From: Bob <sip:bob@biloxi.com>;tag=98765
            To: Alice <sip:alice@atlanta.com>;tag=54321
            Call-ID: testresponse@domain.com
            CSeq: 1 INVITE
            Content-Length: 0
            Max-Forwards: 0

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Expected the SIP response 200 OK to be parsed successfully" +
                        result.mapLeft { it.message }
        )
        result.map { sipMsg ->
            // Verify that the parsed message is a SipResponse
            assertTrue(sipMsg is SipResponse, "Parsed message must be a SipResponse")
            sipMsg as SipResponse
            // Validate the SIP version
            assertEquals(
                    SipVersionValue.SIP_2_0,
                    sipMsg.sipVersion.version,
                    "SIP version should be 2.0"
            )
            // Validate the status code
            assertEquals(200, sipMsg.statusCode, "Status code must be 200")
            // Validate the reason phrase
            assertEquals("OK", sipMsg.reasonPhrase, "Reason phrase should be 'OK'")
        }
    }

    /**
     * Test 5: Verify that a message with an invalid request line returns an error. The request line
     * does not conform to the expected format.
     */
    @Test
    fun testInvalidRequestLine() {
        val message =
                """
            INVALIDLINE
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKmis
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=aaa
            To: Alice <sip:alice@atlanta.com>
            Call-ID: error@domain.com
            CSeq: 1 INVITE

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "A message with an invalid request line must fail parsing")
    }

    /** Test 6: Verify that a message missing a mandatory header (Call-ID) returns an error. */
    @Test
    fun testMissingMandatoryHeader() {
        val message =
                """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKmissing
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=missing
            To: Alice <sip:alice@atlanta.com>
            CSeq: 1 INVITE

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isLeft(),
                "Missing mandatory header (Call-ID) should cause a parsing error"
        )
    }

    /**
     * Test 7: Verify that a message with a malformed Via header returns an error. The Via header
     * does not follow proper syntax.
     */
    @Test
    fun testInvalidViaHeader() {
        val message =
                """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: MALFORMED_VIA_HEADER
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=viaerror
            To: Alice <sip:alice@atlanta.com>
            Call-ID: viaerror@domain.com
            CSeq: 1 INVITE

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "A malformed Via header should trigger a parsing error")
    }

    /** Test 8: Verify that an empty message returns an error. */
    @Test
    fun testEmptyMessage() {
        val message = ""
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "An empty message must return a parsing error")
    }

    /**
     * Test 9: Verify that a message with an invalid CSeq header (non-numeric sequence) returns an
     * error.
     */
    @Test
    fun testInvalidCSeqHeader() {
        val message =
                """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKCSeq
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=cseqerror
            To: Alice <sip:alice@atlanta.com>
            Call-ID: cseqerror@domain.com
            CSeq: abc INVITE

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "A CSeq header with a non-numeric sequence must be rejected")
    }

    /** Test 10: Verify that a message with an unknown SIP method returns an error. */
    @Test
    fun testUnknownSipMethod() {
        val message =
                """
            UNKNOWN sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKunknown
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=unknown
            To: Alice <sip:alice@atlanta.com>
            Call-ID: unknown@domain.com
            CSeq: 1 UNKNOWN

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "An unknown SIP method must trigger a parsing error")
    }

    /** Test 11: Verify that a SIP response missing a reason phrase returns an error. */
    @Test
    fun testResponseMissingReasonPhrase() {
        val message =
                """
            SIP/2.0 400 
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKmissreason
            From: Bob <sip:bob@biloxi.com>;tag=reason
            To: Alice <sip:alice@atlanta.com>
            Call-ID: missreason@domain.com
            CSeq: 1 INVITE
            Content-Length: 0

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "A response missing a reason phrase must cause a parsing error")
    }

    /** Test 12: Verify that a request with an invalid SIP version returns an error. */
    @Test
    fun testInvalidSipVersion() {
        val message =
                """
            INVITE sip:alice@atlanta.com SIP/3.0
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKver
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=ver
            To: Alice <sip:alice@atlanta.com>
            Call-ID: ver@domain.com
            CSeq: 1 INVITE

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "A request with an invalid SIP version must be rejected")
    }

    /**
     * Test 13: Verify that a request with extra spaces is handled correctly. Ensures that trimming
     * occurs and fields are extracted properly.
     */
    @Test
    fun testRequestWithExtraSpaces() {
        val message =
                """
            INVITE     sip:alice@atlanta.com    SIP/2.0
            Via: SIP/2.0/UDP    server10.biloxi.com;branch=z9hG4bKspace
            Max-Forwards:     70
            From:   Bob   <sip:bob@biloxi.com>;tag=space
            To: Alice <sip:alice@atlanta.com>
            Call-ID:   extraspace@domain.com
            CSeq:   1    INVITE

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Extra spaces should be trimmed and message parsed successfully"
        )
        result.map { sipMsg ->
            // Validate that the parsed message is a SipRequest
            assertTrue(sipMsg is SipRequest, "Parsed message must be a SipRequest")
            sipMsg as SipRequest
            // Check that the method is correctly extracted after trimming extra spaces
            assertEquals(
                    SipMethodValue.INVITE,
                    sipMsg.method.method,
                    "Method should be INVITE after trimming"
            )
            // Check that the CSeq sequence number is correctly extracted
            assertEquals(
                    1L,
                    sipMsg.cSeq.sequenceNumber,
                    "CSeq sequence number must be 1 after trimming"
            )
        }
    }

    /** Test 14: Verify that a SIP response with a non-numeric status code returns an error. */
    @Test
    fun testResponseWithNonNumericStatus() {
        val message =
                """
            SIP/2.0 ABC OK
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKnonum
            From: Bob <sip:bob@biloxi.com>;tag=nonum
            To: Alice <sip:alice@atlanta.com>
            Call-ID: nonum@domain.com
            CSeq: 1 INVITE
            Content-Length: 0

        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isLeft(), "A non-numeric status code must trigger a parsing error")
    }

    /**
     * Test 15: Verify that the parser correctly extracts the body of the SIP message. Compares the
     * extracted body with the expected content.
     */
    @Test
    fun testMessageWithBody() {
        val bodyExpected =
                """
            v=0
            o=alice 2890844526 2890844526 IN IP4 host.atlanta.com
            s=Session SDP
            c=IN IP4 host.atlanta.com
            t=0 0
            m=audio 49170 RTP/AVP 0
        """.trimIndent()
        val message =
                """
            INVITE sip:alice@atlanta.com SIP/2.0
            Via: SIP/2.0/UDP server10.biloxi.com;branch=z9hG4bKbody
            Max-Forwards: 70
            From: Bob <sip:bob@biloxi.com>;tag=bodytag
            To: Alice <sip:alice@atlanta.com>
            Call-ID: body@domain.com
            CSeq: 1 INVITE
            Content-Length: 1016

            v=0
            o=alice 2890844526 2890844526 IN IP4 host.atlanta.com
            s=Session SDP
            c=IN IP4 host.atlanta.com
            t=0 0
            m=audio 49170 RTP/AVP 0
        """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "A SIP message with a body should be parsed correctly :${message}"
        )
        result.map { sipMsg ->
            // Concatenate the body lines and check they match the expected content exactly
            val body = sipMsg.body[0]
            assertEquals(
                    bodyExpected,
                    body,
                    "Extracted message body must match the expected content \n ${bodyExpected} ${body}"
            )
        }
    }

    /** Tests from RFC 4475 - SIP Torture Tests */
    @Test
    fun testShortTorturousInvite() {
        val message =
                """
         INVITE sip:vivekg@chair-dnrc.example.com;unknownparam SIP/2.0
         TO : sip:vivekg@chair-dnrc.example.com ; tag = 1918181833n
         From   : "J Rosenberg \""       <sip:jdrosen@example.com>
         Max-Forwards: 0069
         Call-ID: 98asdh@10.1.1.1
         CSeq: 0009 INVITE
         Via: SIP/2.0/UDP 135.180.130.133
         Contact : <sip:jdrosen@example.com>
         
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Parser should handle unusual spacing and line folding in headers"
        )
    }

    @Test
    fun testWideRangeOfValidCharacters() {
        val message =
                """
         !interesting-Method0123456789_*+`.%indeed'~ sip:1_unusual.URI~(to-be!sure)&isn't+it$/crazy?,/;;*:&it+has=1,weird!*paswo~d_too.(doesn't-it)@example.com SIP/2.0
         Via: SIP/2.0/UDP host1.example.com
         To: sip:user@example.com
         From: sip:caller@example.net;tag=29342
         Max-Forwards: 70
         Call-ID: test.wide@range.chars
         CSeq: 1 !interesting-Method0123456789_*+`.%indeed'~
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Parser should accept wide range of valid characters in method name and request-URI ${result.mapLeft { it.message }}"
        )
    }

    @Test
    fun testEscapedPercentagesInURI() {
        val message =
                """
         INVITE sip:sips%3Auser%40example.com@example.net SIP/2.0
         To: sip:user@example.com
         From: sip:caller@example.net;tag=93334
         Max-Forwards: 70
         Call-ID: escaped.percent@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host5.example.net
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "Parser should handle escaped percentages in URIs correctly")
    }

    @Test
    fun testEscapedNullsInURI() {
        val message =
                """
         INVITE sip:null%00@example.com SIP/2.0
         To: sip:user@example.com
         From: sip:caller@example.net;tag=39091
         Max-Forwards: 70
         Call-ID: escaped.null@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host.example.net
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "Parser should handle escaped nulls in URIs")
    }

    @Test
    fun testNoLWSBetweenDisplayNameAndURI() {
        val message =
                """
         INVITE sip:user@example.com SIP/2.0
         To: sip:user@example.com
         From: "Quoted String"<sip:caller@example.net>;tag=5617
         Max-Forwards: 70
         Call-ID: no.lws@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host.example.net
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Parser should handle absence of LWS between display name and URI"
        )
    }

    @Test
    fun testLongValuesInHeaders() {
        val message =
                """
         INVITE sip:user@example.com SIP/2.0
         To: "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" <sip:user@example.com>
         From: "Long String"<sip:caller@example.net>;tag=986876
         Call-ID: long.values@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host.example.net
         Max-Forwards: 70
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "Parser should handle very long values in header fields")
    }

    @Test
    fun testMultipartMimeMessage() {
        val message =
                """
         INVITE sip:user@example.com SIP/2.0
         To: sip:user@example.com
         From: sip:caller@example.net;tag=8814
         Call-ID: multipart.mime@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host.example.net
         Max-Forwards: 70
         Content-Type: multipart/mixed;boundary=boundary42
         
         --boundary42
         Content-Type: text/plain
         
         Hello World
         --boundary42
         Content-Type: application/sdp
         
         v=0
         o=alice 2890844526 2890844526 IN IP4 host.atlanta.com
         s=-
         c=IN IP4 host.atlanta.com
         t=0 0
         m=audio 49170 RTP/AVP 0
         --boundary42--
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "Parser should handle multipart MIME messages correctly")
    }

    @Test
    fun testUnusualReasonPhrase() {
        val message =
                """
         SIP/2.0 200 = 2**3 * 5**2 но сто девяносто девять - просто просто
         To: sip:user@example.com
         From: sip:caller@example.net;tag=8814
         Call-ID: unusual.reason@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host.example.net
         Max-Forwards: 70
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "Parser should handle unusual characters in reason phrase")
    }

    @Test
    fun testEmptyReasonPhrase() {
        val message =
                """
         SIP/2.0 200 
         To: sip:user@example.com
         From: sip:caller@example.net;tag=7643
         Call-ID: empty.reason@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host.example.net
         Max-Forwards: 70
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(result.isRight(), "Parser should handle empty reason phrase ${result.mapLeft { it.message }}")
    }

    @Test
    fun testSemicolonSeparatedParamsInURIUserPart() {
        val message =
                """
         INVITE sip:user;par=u%40example.net@example.com SIP/2.0
         To: sip:j_user@example.com
         From: sip:caller@example.net;tag=93334
         Max-Forwards: 70
         Call-ID: semicolon.params@test.com
         CSeq: 1 INVITE
         Via: SIP/2.0/UDP host1.example.com
     """.trimIndent()
        val result = sipParserService.parse(message)
        assertTrue(
                result.isRight(),
                "Parser should handle semicolon-separated parameters in URI user part"
        )
    }
}

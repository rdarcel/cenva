package org.cenva

import io.quarkus.test.junit.QuarkusTest
import org.cenva.dao.sip.SipParseError

import org.cenva.dao.sip.ContentTypeHeader
import org.cenva.parser.ContentTypeParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import arrow.core.Some
import arrow.core.None

@QuarkusTest
class TestParserContentType {

    private val parser = ContentTypeParser()

    // 1. Test parsing a basic application/sdp header with charset parameter (RFC 3261 example)
    @Test
    fun testApplicationSdpCharset() {
        val input = "application/sdp; charset=UTF-8"
        val result = parser.parse(input)
        assertTrue(result.isRight(), "Expected valid Content-Type: 'application/sdp; charset=UTF-8'")
        result.map { header ->
            assertEquals("application", header.type, "Type should be 'application'")
            assertEquals("sdp", header.subType, "SubType should be 'sdp'")
            assertTrue(header.parameters.containsKey("charset"), "Parameters must contain charset")
            header.parameters["charset"]?.map { value ->
                assertEquals("UTF-8", value, "Charset parameter should be 'UTF-8'")
            }
        }
    }

    // 2. Test parsing a multipart Content-Type with multiple parameters (as in RFC examples)
    @Test
    fun testMultipartRelated() {
        val input = "multipart/related; boundary=--myboundary; type=application/sdp"
        val result = parser.parse(input)
        assertTrue(result.isRight(), "Expected valid Content-Type: 'multipart/related; boundary=--myboundary; type=application/sdp'")
        result.map { header ->
            assertEquals("multipart", header.type, "Type should be 'multipart'")
            assertEquals("related", header.subType, "SubType should be 'related'")
            assertTrue(header.parameters.containsKey("boundary"), "Parameters must contain 'boundary'")
            header.parameters["boundary"]?.map { value ->
                assertEquals("--myboundary", value, "Boundary parameter should be '--myboundary'")
            }
            assertTrue(header.parameters.containsKey("type"), "Parameters must contain 'type'")
            header.parameters["type"]?.map { value ->
                assertEquals("application/sdp", value, "Type parameter should be 'application/sdp'")
            }
        }
    }

    // 3. Test parsing a simple Content-Type without any parameter.
    @Test
    fun testSimpleContentType() {
        val input = "application/json"
        val result = parser.parse(input)
        assertTrue(result.isRight(), "Expected valid Content-Type: 'application/json'")
        result.map { header ->
            assertEquals("application", header.type, "Type should be 'application'")
            assertEquals("json", header.subType, "SubType should be 'json'")
            assertTrue(header.parameters.isEmpty(), "There should be no parameters")
        }
    }

    // 4. Test parsing a text-based Content-Type with a parameter (e.g., text/plain; format=flowed).
    @Test
    fun testTextPlainFormatFlowed() {
        val input = "text/plain; format=flowed"
        val result = parser.parse(input)
        assertTrue(result.isRight(), "Expected valid Content-Type: 'text/plain; format=flowed'")
        result.map { header ->
            assertEquals("text", header.type, "Type should be 'text'")
            assertEquals("plain", header.subType, "SubType should be 'plain'")
            assertTrue(header.parameters.containsKey("format"), "Parameters must contain 'format'")
            header.parameters["format"]?.map { value ->
                assertEquals("flowed", value, "Format parameter should be 'flowed'")
            }
        }
    }

    // 5. Test parsing with extra whitespace (should trim spaces correctly)
    @Test
    fun testContentTypeWithExtraSpaces() {
        val input = "  application / sdp  ;  charset =  UTF-8  "
        val result = parser.parse(input)
        assertTrue(result.isRight(), "Expected valid Content-Type with extra spaces")
        result.map { header ->
            assertEquals("application", header.type, "Type should be 'application' after trimming")
            assertEquals("sdp", header.subType, "SubType should be 'sdp' after trimming")
            assertTrue(header.parameters.containsKey("charset"), "Parameters must include 'charset'")
            header.parameters["charset"]?.map { value ->
                assertEquals("UTF-8", value, "Charset parameter should be 'UTF-8'")
            }
        }
    }

    // 6. Test parsing with a parameter that has no value (e.g., key without value)
    @Test
    fun testParameterWithoutValue() {
        val input = "application/sdp; boundary"
        val result = parser.parse(input)
        assertTrue(result.isRight(), "Expected valid Content-Type even if parameter has no value")
        result.map { header ->
            assertEquals("application", header.type, "Type should be 'application'")
            assertEquals("sdp", header.subType, "SubType should be 'sdp'")
            assertTrue(header.parameters.containsKey("boundary"), "Parameters must contain 'boundary'")
            header.parameters["boundary"]?.map { value ->
                // If no value, parser may set None or empty string; adjust assertion according to implementation
                assertEquals("", value, "Boundary parameter should be an empty string if no value provided")
            }
        }
    }

    // 7. Test parsing a Content-Type with a quoted parameter value.
    @Test
    fun testQuotedParameterValue() {
        val input = "application/sdp; description=\"Session Description\""
        val result = parser.parse(input)
        assertTrue(result.isRight(), "Expected valid Content-Type with quoted parameter value")
        result.map { header ->
            assertEquals("application", header.type, "Type should be 'application'")
            assertEquals("sdp", header.subType, "SubType should be 'sdp'")
            assertTrue(header.parameters.containsKey("description"), "Parameters must contain 'description'")
            header.parameters["description"]?.map { value ->
                assertEquals("Session Description", value, "Description parameter should be 'Session Description'")
            }
        }
    }

    // 8. Test parsing a Content-Type with an invalid format (missing '/')
    @Test
    fun testMissingSlash() {
        val input = "application sdp; charset=UTF-8"
        val result = parser.parse(input)
        assertTrue(result.isLeft(), "Parsing should fail when '/' is missing between type and subtype")
    }

    // 9. Test parsing an empty Content-Type header (should return error)
    @Test
    fun testEmptyContentType() {
        val input = ""
        val result = parser.parse(input)
        assertTrue(result.isLeft(), "Parsing should fail for an empty Content-Type header")
    }

    // 10. Test serialization of a ContentTypeHeader object back to a string.
    @Test
    fun testToStringSerialization() {
        val input = "application/sdp; charset=UTF-8; format=mandatory"
        val parseResult = parser.parse(input)
        assertTrue(parseResult.isRight(), "Expected valid Content-Type for serialization test")
        if(parseResult.isRight()) {
            val header = parseResult.getOrNull()!!
            val serialized = parser.toString(header)
            // Verify that the serialized result contains the type/subtype and parameters.
            assertEquals(serialized,input, "Serialized string must be equal to input'")
         
        }
       
    }
}
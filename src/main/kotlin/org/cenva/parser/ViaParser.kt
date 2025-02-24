package org.cenva.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.cenva.dao.sip.SipObject
import org.cenva.dao.sip.SipParseError
import org.cenva.dao.sip.ViaHeader
import org.jboss.logging.Logger


/**
 * Parser for Via header fields as specified in RFC 3261
 */
class ViaParser : ISipParserProvider<ViaHeader> {
    
    companion object {
        private val log = Logger.getLogger(ViaParser::class.java)
        
        // Regex to parse Via header: protocol/version/transport host[:port][;parameters]
        private val VIA_REGEX = """^([^/\s]+)/([^/\s]+)/([^\s]+)\s+(\[(?:[^\]]*)]|(?:(?:(?:25[0-5]|(?:2[0-4]|1\d|[1-9]|)\d)\.?\b){4})|(?:(?:(?!-:)[A-Za-z0-9\-]{1,255}(?<!-:)\.)+[A-Za-z]{2,6}))(?::(\d+))?(?:\s+)?(?:;(.*))?$""".toRegex()

        //^([^/\s]+)/([^/\s]+)/([^\s]+)\s+([^:;\s]+)(?::(\d+))?(?:\s+)?(?:;(.*))?$
        //^([^/\s]+)/([^/\s]+)/([^\s]+)\s+(\[(?:[^\]]*)]|(?:(?:(?:25[0-5]|(?:2[0-4]|1\d|[1-9]|)\d)\.?\b){4})|(?:(?:(?!-)[A-Za-z0–9-]{1,63}(?<!-)\.)+[A-Za-z]{2,6}))(?::(\d+))?(?:\s+)?(?:;(.*))?$
        
    
        
        // Regex for Via parameters
        private val PARAM_REGEX = """([^;=]+)(?:=([^;]+))?""".toRegex()


    }

    /**
     * Parse a Via header field
     * @param message the Via header field to parse
     * @return Either an error or a ViaHeader object
     */
    override fun parse(message: String): Either<SipParseError, ViaHeader> {
        val match = VIA_REGEX.find(message.trim()) 
            ?: return Either.Left(SipParseError.InvalidFormat("Invalid Via format: $message"))

        val (protocol, version, transport, host, portStr, paramsStr) = match.destructured

        // Parse port if present
        val port = portStr.takeIf { it.isNotEmpty() }?.toIntOrNull()?.let { Some(it) } ?: None
        
        var branch : Option<String> = None
  
        // Parse parameters
        val parameters = mutableMapOf<String, Option<String>>()
        PARAM_REGEX.findAll(paramsStr).forEach { paramMatch ->
            val (name, value) = paramMatch.destructured
            parameters[name] = if (value.isNotEmpty()) Some(value) else None
            if(name == "branch" && value.isNotEmpty()) {
                branch = Some(value)
            }
        }
   

        return Either.Right(
            ViaHeader(
                protocol = "$protocol/$version",
                transport = transport,
                host = host,
                port = port,
                parameters = parameters,
                branch = branch
            )
        )
    }

    /**
     * Convert a ViaHeader object to a string
     */
    override fun toString(obj: ViaHeader): String {
        val sb = StringBuilder()
        
        // Add protocol and transport
        sb.append(obj.protocol)
        sb.append("/")
        sb.append(obj.transport)
        sb.append(" ")
        
        // Add host and optional port
        sb.append(obj.host)
        obj.port.map { sb.append(":").append(it) }
        
        // Add parameters
        obj.parameters.forEach { (name, value) ->
            sb.append(";").append(name)
            value.map { sb.append("=").append(it) }
        }
        
        return sb.toString()
    }
}
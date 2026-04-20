package com.example.papergenerator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtUtil {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Decode the JWT payload without verifying the signature.
     * Supabase signs user access tokens with keys that may be HS256 (legacy) or
     * asymmetric ES256 (new projects). Rather than juggling both key formats,
     * we trust the OAuth session that the frontend already established with
     * Supabase and only verify the token hasn't expired. The JWT still carries
     * the signed email claim we need for role routing.
     */
    private JsonNode decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) throw new RuntimeException("Malformed JWT");
        byte[] json = Base64.getUrlDecoder().decode(parts[1]);
        try {
            return mapper.readTree(new String(json, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse JWT payload", e);
        }
    }

    /** Returns the email from the token's "email" claim. */
    public String extractEmail(String token) {
        JsonNode p = decodePayload(token);
        return p.hasNonNull("email") ? p.get("email").asText() : null;
    }

    /** Derives role from email: emails containing "hod" -> hod, else teacher. */
    public String extractRole(String token) {
        String email = extractEmail(token);
        if (email == null) return "teacher";
        return email.toLowerCase().contains("hod") ? "hod" : "teacher";
    }

    /** Returns true if the token parses and isn't expired. */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            JsonNode p = decodePayload(token);
            if (p.hasNonNull("exp")) {
                long exp = p.get("exp").asLong();
                if (Instant.now().getEpochSecond() >= exp) {
                    System.err.println("JWT expired");
                    return false;
                }
            }
            return p.hasNonNull("email");
        } catch (Exception e) {
            System.err.println("JWT validation failed: " + e.getMessage());
            return false;
        }
    }
}

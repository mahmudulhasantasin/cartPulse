package com.cartpulse.security;

import com.cartpulse.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Pure Java HMAC-SHA256 JWT provider for stateless cross-origin authentication
 * between Vercel (Frontend) and Render (Backend).
 */
@Component
public class JwtTokenProvider {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final Base64.Encoder B64_URL_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DEC = Base64.getUrlDecoder();

    private final byte[] secretBytes;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:cartpulse-dev-secret-key-change-in-production-2026}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(Long userId, String email, Role role) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expirationSeconds;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"uid\":%d,\"role\":\"%s\",\"iat\":%d,\"exp\":%d}",
                escapeJson(email), userId, role.name(), now, exp
        );

        String headerB64 = B64_URL_ENC.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = B64_URL_ENC.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;
        String signatureB64 = sign(signingInput);

        return signingInput + "." + signatureB64;
    }

    public String validateAndGetSubject(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = sign(signingInput);
        if (!MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        String payload = new String(B64_URL_DEC.decode(parts[1]), StandardCharsets.UTF_8);
        long exp = extractLongClaim(payload, "exp");
        if (Instant.now().getEpochSecond() > exp) {
            return null;
        }
        return extractStringClaim(payload, "sub");
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALG));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return B64_URL_ENC.encodeToString(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign JWT", ex);
        }
    }

    private static String extractStringClaim(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return null;
        int valStart = start + marker.length();
        int valEnd = json.indexOf('"', valStart);
        return valEnd > valStart ? json.substring(valStart, valEnd) : null;
    }

    private static long extractLongClaim(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return 0L;
        int valStart = start + marker.length();
        int valEnd = valStart;
        while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) {
            valEnd++;
        }
        return valEnd > valStart ? Long.parseLong(json.substring(valStart, valEnd)) : 0L;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

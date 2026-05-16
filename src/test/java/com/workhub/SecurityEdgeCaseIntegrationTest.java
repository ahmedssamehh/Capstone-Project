package com.workhub;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityEdgeCaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void malformedTokenReturnsUnified401() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer malformed.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void expiredTokenReturnsUnified401() throws Exception {
        String token = buildToken(
                Map.of("userId", 1L, "tenantId", 1L, "role", "TENANT_ADMIN"),
                "admin@demo.com",
                new Date(System.currentTimeMillis() - 120_000)
        );

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void tokenMissingTenantClaimReturnsUnified401() throws Exception {
        String token = buildToken(
                Map.of("userId", 1L, "role", "TENANT_ADMIN"),
                "admin@demo.com",
                new Date(System.currentTimeMillis() + 300_000)
        );

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void tokenMissingRoleClaimReturnsUnified401() throws Exception {
        String token = buildToken(
                Map.of("userId", 1L, "tenantId", 1L),
                "admin@demo.com",
                new Date(System.currentTimeMillis() + 300_000)
        );

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void tamperedTokenReturnsUnified401() throws Exception {
        String validToken = buildToken(
                Map.of("userId", 1L, "tenantId", 1L, "role", "TENANT_ADMIN"),
                "admin@demo.com",
                new Date(System.currentTimeMillis() + 300_000)
        );
        String tampered = validToken.substring(0, validToken.length() - 2) + "ab";

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    private String buildToken(Map<String, Object> claims, String subject, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> payload = new HashMap<>(claims);
        Date now = new Date();
        return Jwts.builder()
                .setClaims(payload)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}

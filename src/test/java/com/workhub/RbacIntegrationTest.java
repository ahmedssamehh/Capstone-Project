package com.workhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload("RBACMISSING")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload("RBACINVALID")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void insufficientRoleReturns403() throws Exception {
        String userToken = login("user@demo.com", "user123");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload("RBACUSER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void adminRoleSucceeds() throws Exception {
        String adminToken = login("admin@demo.com", "admin123");
        String uniqueKey = "RB" + (System.nanoTime() % 100000000);

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload(uniqueKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.projectKey").value(uniqueKey))
                .andExpect(jsonPath("$.name").value("RBAC Project"));
    }

    private String login(String email, String password) throws Exception {
        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.path("token").asText();
    }

    private String projectPayload(String projectKey) {
        return """
                {
                  "name": "RBAC Project",
                  "description": "RBAC integration test payload",
                  "projectKey": "%s"
                }
                """.formatted(projectKey);
    }
}

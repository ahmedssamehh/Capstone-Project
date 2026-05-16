package com.workhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhub.entity.Project;
import com.workhub.entity.Task;
import com.workhub.entity.Tenant;
import com.workhub.entity.User;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TaskRepository;
import com.workhub.repository.TenantRepository;
import com.workhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long tenantOneProjectId;
    private Long tenantOneTaskId;
    private String tenantTwoAdminToken;
    private String tenantTwoUserToken;

    @BeforeEach
    void setupTenantsData() throws Exception {
        Tenant tenantOne = tenantRepository.findById(1L).orElseThrow();
        User tenantOneAdmin = userRepository.findByEmail("admin@demo.com").orElseThrow();

        Project tenantOneProject = projectRepository.findByTenantIdAndProjectKey(1L, "T1ISO")
                .orElseGet(() -> projectRepository.save(Project.builder()
                        .tenant(tenantOne)
                        .createdBy(tenantOneAdmin)
                        .name("Tenant One Project")
                        .description("Isolation baseline")
                        .projectKey("T1ISO")
                        .status(Project.ProjectStatus.ACTIVE)
                        .build()));

        Task tenantOneTask = taskRepository.searchByTitleInTenant(1L, "Tenant One Task")
                .stream()
                .findFirst()
                .orElseGet(() -> taskRepository.save(Task.builder()
                        .tenant(tenantOne)
                        .project(tenantOneProject)
                        .assignedTo(tenantOneAdmin)
                        .title("Tenant One Task")
                        .description("Isolation baseline task")
                        .status(Task.TaskStatus.TODO)
                        .priority(Task.TaskPriority.MEDIUM)
                        .build()));

        tenantOneProjectId = tenantOneProject.getId();
        tenantOneTaskId = tenantOneTask.getId();

        if (userRepository.findByEmail("tenant2.admin@demo.com").isPresent()) {
            Tenant tenantTwo = tenantRepository.findByNameIgnoreCase("Second Company").orElseThrow();
            User tenantTwoAdmin = userRepository.findByEmail("tenant2.admin@demo.com").orElseThrow();
            projectRepository.findByTenantIdAndProjectKey(tenantTwo.getId(), "T2ISO")
                    .orElseGet(() -> projectRepository.save(Project.builder()
                            .tenant(tenantTwo)
                            .createdBy(tenantTwoAdmin)
                            .name("Tenant Two Project")
                            .description("Isolation list control")
                            .projectKey("T2ISO")
                            .status(Project.ProjectStatus.ACTIVE)
                            .build()));
            tenantTwoAdminToken = login("tenant2.admin@demo.com", "admin123");
            tenantTwoUserToken = login("tenant2.user@demo.com", "user123");
            return;
        }

        Tenant tenantTwo = tenantRepository.save(Tenant.builder()
                .name("Second Company")
                .plan(Tenant.TenantPlan.FREE)
                .status(Tenant.TenantStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .tenant(tenantTwo)
                .email("tenant2.admin@demo.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Second")
                .lastName("Admin")
                .role(User.UserRole.TENANT_ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .tenant(tenantTwo)
                .email("tenant2.user@demo.com")
                .password(passwordEncoder.encode("user123"))
                .firstName("Second")
                .lastName("User")
                .role(User.UserRole.TENANT_USER)
                .status(User.UserStatus.ACTIVE)
                .build());

        User tenantTwoAdmin = userRepository.findByEmail("tenant2.admin@demo.com").orElseThrow();
        projectRepository.findByTenantIdAndProjectKey(tenantTwo.getId(), "T2ISO")
                .orElseGet(() -> projectRepository.save(Project.builder()
                        .tenant(tenantTwo)
                        .createdBy(tenantTwoAdmin)
                        .name("Tenant Two Project")
                        .description("Isolation list control")
                        .projectKey("T2ISO")
                        .status(Project.ProjectStatus.ACTIVE)
                        .build()));

        tenantTwoAdminToken = login("tenant2.admin@demo.com", "admin123");
        tenantTwoUserToken = login("tenant2.user@demo.com", "user123");
    }

    @Test
    void crossTenantReadBlocked() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", tenantOneProjectId)
                        .header("Authorization", "Bearer " + tenantTwoAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found"))
                .andExpect(jsonPath("$.path").value("/api/projects/" + tenantOneProjectId))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void crossTenantUpdateBlocked() throws Exception {
        String body = """
                {
                  "status": "IN_PROGRESS",
                  "actualHours": 2
                }
                """;

        mockMvc.perform(patch("/api/tasks/{id}", tenantOneTaskId)
                        .header("Authorization", "Bearer " + tenantTwoAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/" + tenantOneTaskId))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void crossTenantListIsolation() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + tenantTwoAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasSize(0))))
                .andExpect(content().string(not(containsString("\"id\":" + tenantOneProjectId))));
    }

    @Test
    void unauthorizedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/api/projects"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void forbiddenRoleReturns403() throws Exception {
        String createProjectBody = """
                {
                  "name": "Forbidden Project",
                  "description": "Role test",
                  "projectKey": "FBDN"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + tenantTwoUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/projects"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void crossTenantUserReadBlocked() throws Exception {
        // Tenant 1 admin has ID=1 from DataInitializer
        mockMvc.perform(get("/api/users/{id}", 1L)
                        .header("Authorization", "Bearer " + tenantTwoAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void crossTenantUserListIsolation() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + tenantTwoAdminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("\"email\":\"admin@demo.com\""))))
                .andExpect(content().string(not(containsString("\"email\":\"user@demo.com\""))));
    }

    @Test
    void crossTenantDeleteBlocked() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/tasks/{id}", tenantOneTaskId)
                        .header("Authorization", "Bearer " + tenantTwoAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found"));
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
}

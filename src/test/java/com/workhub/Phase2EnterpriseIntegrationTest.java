package com.workhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import com.workhub.entity.ProcessedMessage;
import com.workhub.entity.ProcessedMessageStatus;
import com.workhub.entity.Project;
import com.workhub.entity.Task;
import com.workhub.entity.Tenant;
import com.workhub.entity.User;
import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.repository.JobRepository;
import com.workhub.repository.ProcessedMessageRepository;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TaskRepository;
import com.workhub.repository.TenantRepository;
import com.workhub.repository.UserRepository;
import com.workhub.service.impl.ReportGenerationConsumerServiceImpl;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(OutputCaptureExtension.class)
class Phase2EnterpriseIntegrationTest {

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management")
            .withUser("workhub", "workhub");

    @DynamicPropertySource
    static void registerRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "workhub");
        registry.add("spring.rabbitmq.password", () -> "workhub");
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> true);
        registry.add("workhub.rabbitmq.exchange", () -> "workhub.jobs.direct");
        registry.add("workhub.rabbitmq.queue", () -> "workhub.jobs.queue");
        registry.add("workhub.rabbitmq.routing-key", () -> "workhub.jobs.process");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @SpyBean
    private ReportGenerationConsumerServiceImpl reportGenerationConsumerService;

    private Long tenantOneId;
    private Long tenantOneProjectId;
    private Long tenantOneTaskId;
    private Long tenantTwoId;
    private String tenantTwoAdminToken;
    private String tenantTwoUserToken;
    private String tenantOneAdminToken;

    @BeforeEach
    void setup() throws Exception {
        processedMessageRepository.deleteAll();
        jobRepository.deleteAll();

        Tenant tenantOne = tenantRepository.findById(1L).orElseThrow();
        User tenantOneAdmin = userRepository.findByEmail("admin@demo.com").orElseThrow();
        tenantOneId = tenantOne.getId();

        Project tenantOneProject = projectRepository.findByTenantIdAndProjectKey(tenantOneId, "P2T1")
                .orElseGet(() -> projectRepository.save(Project.builder()
                        .tenant(tenantOne)
                        .createdBy(tenantOneAdmin)
                        .name("Phase2 Tenant One Project")
                        .description("Phase2 baseline")
                        .projectKey("P2T1")
                        .status(Project.ProjectStatus.ACTIVE)
                        .build()));
        tenantOneProjectId = tenantOneProject.getId();

        Task tenantOneTask = taskRepository.searchByTitleInTenant(tenantOneId, "Phase2 Tenant One Task")
                .stream()
                .findFirst()
                .orElseGet(() -> taskRepository.save(Task.builder()
                        .tenant(tenantOne)
                        .project(tenantOneProject)
                        .assignedTo(tenantOneAdmin)
                        .title("Phase2 Tenant One Task")
                        .description("Phase2 update target")
                        .status(Task.TaskStatus.TODO)
                        .priority(Task.TaskPriority.MEDIUM)
                        .build()));
        tenantOneTaskId = tenantOneTask.getId();

        Tenant tenantTwo = tenantRepository.findByNameIgnoreCase("Phase2 Second Company")
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .name("Phase2 Second Company")
                        .plan(Tenant.TenantPlan.FREE)
                        .status(Tenant.TenantStatus.ACTIVE)
                        .build()));
        tenantTwoId = tenantTwo.getId();

        User tenantTwoAdmin = userRepository.findByEmail("phase2.admin@demo.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .tenant(tenantTwo)
                        .email("phase2.admin@demo.com")
                        .password(passwordEncoder.encode("admin123"))
                        .firstName("Phase2")
                        .lastName("Admin")
                        .role(User.UserRole.TENANT_ADMIN)
                        .status(User.UserStatus.ACTIVE)
                        .build()));

        userRepository.findByEmail("phase2.user@demo.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .tenant(tenantTwo)
                        .email("phase2.user@demo.com")
                        .password(passwordEncoder.encode("user123"))
                        .firstName("Phase2")
                        .lastName("User")
                        .role(User.UserRole.TENANT_USER)
                        .status(User.UserStatus.ACTIVE)
                        .build()));

        projectRepository.findByTenantIdAndProjectKey(tenantTwoId, "P2T2")
                .orElseGet(() -> projectRepository.save(Project.builder()
                        .tenant(tenantTwo)
                        .createdBy(tenantTwoAdmin)
                        .name("Phase2 Tenant Two Project")
                        .description("Phase2 list isolation")
                        .projectKey("P2T2")
                        .status(Project.ProjectStatus.ACTIVE)
                        .build()));

        tenantTwoAdminToken = login("phase2.admin@demo.com", "admin123");
        tenantTwoUserToken = login("phase2.user@demo.com", "user123");
        tenantOneAdminToken = login("admin@demo.com", "admin123");
    }

    @Test
    void crossTenantReadBlocked() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", tenantOneProjectId)
                        .header("Authorization", "Bearer " + tenantTwoAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found"));
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
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found"));
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
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content("""
                                {"name":"P2 Missing Token","projectKey":"P2M"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType("application/json")
                        .content("""
                                {"name":"P2 Invalid Token","projectKey":"P2I"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void insufficientRoleReturns403() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + tenantTwoUserToken)
                        .contentType("application/json")
                        .content("""
                                {"name":"Forbidden","projectKey":"P2F"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void reportRequestCreatesAsyncJob() throws Exception {
        String response = mockMvc.perform(post("/api/projects/{id}/generate-report", tenantOneProjectId)
                        .header("Authorization", "Bearer " + tenantOneAdminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.projectId").value(tenantOneProjectId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        Long jobId = json.path("id").asLong();
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Job job = jobRepository.findByIdAndTenant_Id(jobId, tenantOneId).orElseThrow();
                    org.assertj.core.api.Assertions.assertThat(job.getStatus())
                            .isIn(JobStatus.PENDING, JobStatus.PROCESSING, JobStatus.COMPLETED);
                });
    }

    @Test
    void rabbitMqEventPublished() throws Exception {
        Long jobId = enqueueReportJobAndGetJobId();
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Optional<ProcessedMessage> processed = processedMessageRepository.findTopByJobIdOrderByFirstReceivedAtDesc(jobId);
                    org.assertj.core.api.Assertions.assertThat(processed).isPresent();
                    org.assertj.core.api.Assertions.assertThat(processed.get().getEventId()).isNotBlank();
                });
    }

    @Test
    void consumerProcessesEvent() throws Exception {
        Long jobId = enqueueReportJobAndGetJobId();
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    Job job = jobRepository.findByIdAndTenant_Id(jobId, tenantOneId).orElseThrow();
                    org.assertj.core.api.Assertions.assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
                });
    }

    @Test
    void duplicateEventIgnored() throws Exception {
        Long jobId = enqueueReportJobAndGetJobId();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Job job = jobRepository.findByIdAndTenant_Id(jobId, tenantOneId).orElseThrow();
                    org.assertj.core.api.Assertions.assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
                });

        ProcessedMessage processed = processedMessageRepository.findTopByJobIdOrderByFirstReceivedAtDesc(jobId).orElseThrow();
        Integer attemptsBefore = processed.getAttemptCount();

        ReportGenerationEvent duplicate = ReportGenerationEvent.builder()
                .eventId(processed.getEventId())
                .jobId(jobId)
                .tenantId(tenantOneId)
                .projectId(tenantOneProjectId)
                .timestamp(java.time.OffsetDateTime.now())
                .correlationId(processed.getCorrelationId())
                .build();
        rabbitTemplate.convertAndSend("workhub.jobs.direct", "workhub.jobs.process", duplicate);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    ProcessedMessage refreshed = processedMessageRepository.findByEventId(processed.getEventId()).orElseThrow();
                    org.assertj.core.api.Assertions.assertThat(refreshed.getAttemptCount()).isEqualTo(attemptsBefore);
                    org.assertj.core.api.Assertions.assertThat(refreshed.getStatus()).isEqualTo(ProcessedMessageStatus.COMPLETED);
                });
    }

    @Test
    void failedProcessingHandledSafely() throws Exception {
        String failEventId = "fail-" + UUID.randomUUID();
        Job job = jobRepository.save(Job.builder()
                .tenant(tenantRepository.getReferenceById(tenantOneId))
                .project(projectRepository.getReferenceById(tenantOneProjectId))
                .status(JobStatus.PENDING)
                .correlationId("corr-fail-" + UUID.randomUUID())
                .build());

        doThrow(new RuntimeException("forced-report-failure"))
                .when(reportGenerationConsumerService)
                .executeBusinessWorkflow(ArgumentMatchers.argThat(e -> failEventId.equals(e.getEventId())));

        ReportGenerationEvent failingEvent = ReportGenerationEvent.builder()
                .eventId(failEventId)
                .jobId(job.getId())
                .tenantId(tenantOneId)
                .projectId(tenantOneProjectId)
                .timestamp(java.time.OffsetDateTime.now())
                .correlationId(job.getCorrelationId())
                .build();

        rabbitTemplate.convertAndSend("workhub.jobs.direct", "workhub.jobs.process", failingEvent);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Job refreshedJob = jobRepository.findByIdAndTenant_Id(job.getId(), tenantOneId).orElseThrow();
                    org.assertj.core.api.Assertions.assertThat(refreshedJob.getStatus()).isEqualTo(JobStatus.FAILED);
                    org.assertj.core.api.Assertions.assertThat(refreshedJob.getErrorMessage()).contains("forced-report-failure");

                    ProcessedMessage message = processedMessageRepository.findByEventId(failEventId).orElseThrow();
                    org.assertj.core.api.Assertions.assertThat(message.getStatus()).isEqualTo(ProcessedMessageStatus.FAILED);
                    org.assertj.core.api.Assertions.assertThat(message.getLastError()).contains("forced-report-failure");
                });
    }

    @Test
    void actuatorEndpointsAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isOk());
    }

    @Test
    void correlationIdsPresentInLogs(CapturedOutput output) throws Exception {
        String correlationId = "corr-phase2-" + UUID.randomUUID();
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + tenantOneAdminToken)
                        .header("X-Correlation-Id", correlationId))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    org.assertj.core.api.Assertions.assertThat(output.getOut()).contains("corr=" + correlationId);
                    org.assertj.core.api.Assertions.assertThat(output.getOut()).contains("request-start");
                });
    }

    private Long enqueueReportJobAndGetJobId() throws Exception {
        String response = mockMvc.perform(post("/api/projects/{id}/generate-report", tenantOneProjectId)
                        .header("Authorization", "Bearer " + tenantOneAdminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.path("token").asText();
    }
}

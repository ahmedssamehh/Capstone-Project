package com.workhub.service;

import com.workhub.entity.Project;
import com.workhub.entity.Task;
import com.workhub.entity.Tenant;
import com.workhub.entity.User;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TaskRepository;
import com.workhub.repository.TenantRepository;
import com.workhub.repository.UserRepository;
import com.workhub.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional Test Service
 * 
 * Demonstrates transaction rollback behavior.
 * 
 * This service intentionally throws an exception after creating
 * a project and task to demonstrate that @Transactional causes
 * everything to rollback.
 * 
 * TESTING:
 * 1. Call the API endpoint
 * 2. Exception is thrown
 * 3. Check database - NOTHING should be saved
 * 4. Both project and task are rolled back
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionalTestService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    /**
     * Test transaction rollback
     * 
     * This method:
     * 1. Creates a project
     * 2. Creates a task for that project
     * 3. Throws an exception intentionally
     * 
     * Result: Everything rolls back - nothing saved to database
     * 
     * @param userId User ID for creating entities
     * @return Never returns (always throws exception)
     * @throws RuntimeException Always thrown to trigger rollback
     */
    @Transactional
    public void testTransactionRollback(Long userId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        
        log.info("=== STARTING TRANSACTIONAL TEST ===");
        log.info("Tenant ID: {}", tenantId);
        log.info("User ID: {}", userId);
        
        // Get tenant and user (with validation)
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // STEP 1: Create a project
        Project project = Project.builder()
                .tenant(tenant)
                .createdBy(user)
                .name("TEST PROJECT - SHOULD BE ROLLED BACK")
                .description("This project should NOT exist in the database after the transaction")
                .projectKey("ROLLBACK")
                .status(Project.ProjectStatus.ACTIVE)
                .build();
        
        Project savedProject = projectRepository.save(project);
        log.info("✅ STEP 1: Created project with ID: {}", savedProject.getId());
        log.info("   Project name: {}", savedProject.getName());
        log.info("   Project key: {}", savedProject.getProjectKey());
        
        // STEP 2: Create a task for that project
        Task task = Task.builder()
                .tenant(tenant)
                .project(savedProject)
                .assignedTo(user)
                .title("TEST TASK - SHOULD BE ROLLED BACK")
                .description("This task should NOT exist in the database after the transaction")
                .status(Task.TaskStatus.TODO)
                .priority(Task.TaskPriority.HIGH)
                .build();
        
        Task savedTask = taskRepository.save(task);
        log.info("✅ STEP 2: Created task with ID: {}", savedTask.getId());
        log.info("   Task title: {}", savedTask.getTitle());
        log.info("   Task status: {}", savedTask.getStatus());
        
        // STEP 3: Throw exception to trigger rollback
        log.error("❌ STEP 3: Throwing exception to trigger rollback...");
        log.error("=== TRANSACTION WILL ROLLBACK ===");
        log.error("Expected result: Project ID {} and Task ID {} should NOT exist in database", 
                 savedProject.getId(), savedTask.getId());
        
        throw new RuntimeException("INTENTIONAL EXCEPTION FOR TRANSACTION ROLLBACK TEST - " +
                "Project ID: " + savedProject.getId() + ", Task ID: " + savedTask.getId());
    }

    /**
     * Verify rollback by checking if entities exist
     * 
     * This method checks if the project and task were actually rolled back.
     * Call this AFTER the testTransactionRollback fails.
     * 
     * @param projectId Project ID that should NOT exist
     * @param taskId Task ID that should NOT exist
     * @return Verification result
     */
    @Transactional(readOnly = true)
    public RollbackVerificationResult verifyRollback(Long projectId, Long taskId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        
        log.info("=== VERIFYING ROLLBACK ===");
        log.info("Checking if Project ID {} exists...", projectId);
        log.info("Checking if Task ID {} exists...", taskId);
        
        boolean projectExists = projectRepository.existsByIdAndTenantId(projectId, tenantId);
        boolean taskExists = taskRepository.existsByIdAndTenantId(taskId, tenantId);
        
        log.info("Project exists: {}", projectExists);
        log.info("Task exists: {}", taskExists);
        
        boolean rollbackSuccessful = !projectExists && !taskExists;
        
        if (rollbackSuccessful) {
            log.info("✅ ROLLBACK SUCCESSFUL - Nothing saved to database");
        } else {
            log.error("❌ ROLLBACK FAILED - Data was saved to database!");
        }
        
        return RollbackVerificationResult.builder()
                .projectId(projectId)
                .taskId(taskId)
                .projectExists(projectExists)
                .taskExists(taskExists)
                .rollbackSuccessful(rollbackSuccessful)
                .message(rollbackSuccessful ? 
                        "Rollback successful - nothing saved to database" : 
                        "Rollback failed - data was saved to database")
                .build();
    }

    /**
     * Result of rollback verification
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RollbackVerificationResult {
        private Long projectId;
        private Long taskId;
        private boolean projectExists;
        private boolean taskExists;
        private boolean rollbackSuccessful;
        private String message;
    }
}

package com.workhub.controller;

import com.workhub.security.CustomUserDetails;
import com.workhub.service.TransactionalTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Transaction Test Controller
 * 
 * IMPORTANT: This controller is for TESTING ONLY!
 * It demonstrates transaction rollback behavior.
 * 
 * DO NOT use in production!
 */
@RestController
@RequestMapping("/api/test/transaction")
@RequiredArgsConstructor
@Slf4j
public class TransactionTestController {

    private final TransactionalTestService transactionalTestService;

    /**
     * Test transaction rollback
     * 
     * POST /api/test/transaction/rollback
     * 
     * This endpoint:
     * 1. Creates a project
     * 2. Creates a task
     * 3. Throws an exception
     * 4. Everything rolls back
     * 
     * MANUAL TESTING STEPS:
     * 
     * 1. Call this endpoint:
     *    POST /api/test/transaction/rollback
     *    Authorization: Bearer <token>
     * 
     * 2. You will get a 500 error (expected!)
     * 
     * 3. Check the database:
     *    SELECT * FROM projects WHERE project_key = 'ROLLBACK';
     *    SELECT * FROM tasks WHERE title LIKE '%ROLLED BACK%';
     * 
     * 4. Result: NOTHING should be found in database
     * 
     * 5. Check logs to see the project ID and task ID that were created
     * 
     * 6. Verify they don't exist:
     *    GET /api/projects/{id}  -> 404 Not Found
     *    GET /api/tasks/{id}     -> 404 Not Found
     * 
     * @param currentUser Authenticated user
     * @return Never returns successfully (always throws exception)
     */
    @PostMapping("/rollback")
    public ResponseEntity<Map<String, Object>> testRollback(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        
        log.info("========================================");
        log.info("TRANSACTION ROLLBACK TEST STARTED");
        log.info("User: {}", currentUser.getEmail());
        log.info("========================================");
        
        try {
            // This will create project and task, then throw exception
            transactionalTestService.testTransactionRollback(currentUser.getId());
            
            // This line should NEVER be reached
            log.error("❌ ERROR: Transaction did not rollback!");
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Transaction did not rollback - this is a bug!"
            ));
            
        } catch (RuntimeException ex) {
            // Expected exception - transaction rolled back
            log.info("========================================");
            log.info("✅ EXCEPTION CAUGHT (Expected)");
            log.info("Exception message: {}", ex.getMessage());
            log.info("========================================");
            log.info("MANUAL VERIFICATION STEPS:");
            log.info("1. Check database for project with key 'ROLLBACK'");
            log.info("2. Check database for task with title containing 'ROLLED BACK'");
            log.info("3. Result: NOTHING should exist in database");
            log.info("========================================");
            
            // Extract IDs from exception message if possible
            String message = ex.getMessage();
            
            // Re-throw to let global exception handler format the response
            throw ex;
        }
    }

    /**
     * Verify rollback
     * 
     * GET /api/test/transaction/verify?projectId=X&taskId=Y
     * 
     * Use this endpoint to verify that the project and task
     * from the rollback test do NOT exist in the database.
     * 
     * @param projectId Project ID to check
     * @param taskId Task ID to check
     * @return Verification result
     */
    @GetMapping("/verify")
    public ResponseEntity<TransactionalTestService.RollbackVerificationResult> verifyRollback(
            @RequestParam Long projectId,
            @RequestParam Long taskId) {
        
        log.info("Verifying rollback for Project ID: {}, Task ID: {}", projectId, taskId);
        
        TransactionalTestService.RollbackVerificationResult result = 
                transactionalTestService.verifyRollback(projectId, taskId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get testing instructions
     * 
     * GET /api/test/transaction/instructions
     * 
     * Returns detailed testing instructions
     */
    @GetMapping("/instructions")
    public ResponseEntity<Map<String, Object>> getInstructions() {
        Map<String, Object> instructions = new HashMap<>();
        
        instructions.put("title", "Transaction Rollback Test Instructions");
        instructions.put("description", "This test demonstrates @Transactional rollback behavior");
        
        instructions.put("steps", new String[]{
            "1. Call POST /api/test/transaction/rollback with valid JWT token",
            "2. The endpoint will create a project and task, then throw an exception",
            "3. You will receive a 500 error response (this is expected!)",
            "4. Check the error response for project ID and task ID",
            "5. Verify in database: SELECT * FROM projects WHERE project_key = 'ROLLBACK'",
            "6. Verify in database: SELECT * FROM tasks WHERE title LIKE '%ROLLED BACK%'",
            "7. Result: NOTHING should be found (rollback successful)",
            "8. Alternative: Use GET /api/projects/{id} -> should return 404",
            "9. Alternative: Use GET /api/tasks/{id} -> should return 404",
            "10. Use GET /api/test/transaction/verify?projectId=X&taskId=Y to verify"
        });
        
        instructions.put("expected_result", "Both project and task are NOT saved to database");
        instructions.put("sql_verification", new String[]{
            "SELECT * FROM projects WHERE project_key = 'ROLLBACK';  -- Should return 0 rows",
            "SELECT * FROM tasks WHERE title LIKE '%ROLLED BACK%';   -- Should return 0 rows"
        });
        
        instructions.put("curl_example", 
            "curl -X POST http://localhost:8080/api/test/transaction/rollback \\\n" +
            "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\"");
        
        return ResponseEntity.ok(instructions);
    }
}

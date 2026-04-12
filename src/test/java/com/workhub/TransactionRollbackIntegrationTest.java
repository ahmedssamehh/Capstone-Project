package com.workhub;

import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TaskRepository;
import com.workhub.security.TenantContext;
import com.workhub.service.TransactionalTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRollbackIntegrationTest {

    @Autowired
    private TransactionalTestService transactionalTestService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void transactionalWriteRollsBackFullyOnFailure() {
        TenantContext.setTenantId(1L);
        try {
            assertThrows(RuntimeException.class, () -> transactionalTestService.testTransactionRollback(1L));
        } finally {
            TenantContext.clear();
        }

        assertTrue(projectRepository.findByTenantIdAndProjectKey(1L, "ROLLBACK").isEmpty());
        assertTrue(taskRepository.searchByTitleInTenant(1L, "ROLLED BACK").isEmpty());
    }
}

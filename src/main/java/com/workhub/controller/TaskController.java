package com.workhub.controller;

import com.workhub.dto.CreateTaskRequest;
import com.workhub.dto.TaskDto;
import com.workhub.dto.UpdateTaskRequest;
import com.workhub.entity.Task;
import com.workhub.security.TenantContext;
import com.workhub.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskDto task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long id) {
        TaskDto task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllTasks() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<TaskDto> tasks = taskService.getAllTasksByTenant(tenantId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDto>> getTasksByProject(@PathVariable Long projectId) {
        List<TaskDto> tasks = taskService.getAllTasksByProject(projectId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<List<TaskDto>> getTasksByAssignee(@PathVariable Long userId) {
        List<TaskDto> tasks = taskService.getTasksByAssignee(userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/project/{projectId}/status/{status}")
    public ResponseEntity<List<TaskDto>> getTasksByProjectAndStatus(
            @PathVariable Long projectId,
            @PathVariable Task.TaskStatus status) {
        List<TaskDto> tasks = taskService.getTasksByProjectAndStatus(projectId, status);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/assignee/{userId}/status/{status}")
    public ResponseEntity<List<TaskDto>> getTasksByAssigneeAndStatus(
            @PathVariable Long userId,
            @PathVariable Task.TaskStatus status) {
        List<TaskDto> tasks = taskService.getTasksByAssigneeAndStatus(userId, status);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<TaskDto>> getOverdueTasks() {
        List<TaskDto> tasks = taskService.getOverdueTasks();
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskDto task = taskService.updateTask(id, request);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}/count")
    public ResponseEntity<Long> countTasksByProject(@PathVariable Long projectId) {
        long count = taskService.countTasksByProject(projectId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/project/{projectId}/status/{status}/count")
    public ResponseEntity<Long> countTasksByProjectAndStatus(
            @PathVariable Long projectId,
            @PathVariable Task.TaskStatus status) {
        long count = taskService.countTasksByProjectAndStatus(projectId, status);
        return ResponseEntity.ok(count);
    }
}

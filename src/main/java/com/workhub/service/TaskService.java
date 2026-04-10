package com.workhub.service;

import com.workhub.dto.CreateTaskRequest;
import com.workhub.dto.TaskDto;
import com.workhub.dto.UpdateTaskRequest;
import com.workhub.entity.Task;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    
    TaskDto createTask(CreateTaskRequest request);
    
    TaskDto getTaskById(Long id);
    
    List<TaskDto> getAllTasksByProject(Long projectId);
    
    List<TaskDto> getAllTasksByTenant(UUID tenantId);
    
    List<TaskDto> getTasksByAssignee(Long userId);
    
    List<TaskDto> getTasksByProjectAndStatus(Long projectId, Task.TaskStatus status);
    
    List<TaskDto> getTasksByAssigneeAndStatus(Long userId, Task.TaskStatus status);
    
    List<TaskDto> getOverdueTasks();
    
    TaskDto updateTask(Long id, UpdateTaskRequest request);
    
    void deleteTask(Long id);
    
    long countTasksByProject(Long projectId);
    
    long countTasksByProjectAndStatus(Long projectId, Task.TaskStatus status);
}

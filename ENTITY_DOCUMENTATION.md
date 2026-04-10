# JPA Entities Documentation - Multi-Tenant System

## Overview

This document describes the JPA entities for a multi-tenant SaaS system with proper relationships, annotations, and optimistic locking.

## Entity Relationship Diagram

```
┌─────────────────┐
│     Tenant      │
│  (UUID id)      │
│  - name         │
│  - plan         │
└────────┬────────┘
         │
         │ 1:N
         │
    ┌────┴────────────────────┐
    │                         │
    ▼                         ▼
┌─────────────┐      ┌─────────────────┐
│    User     │      │    Project      │
│  (Long id)  │      │   (Long id)     │
│  - email    │      │   - name        │
│  - password │      │   - tenantId    │
│  - role     │◄─────┤   - createdBy   │
│  - tenantId │      └────────┬────────┘
└─────┬───────┘               │
      │                       │ 1:N
      │ 1:N                   │
      │                       ▼
      │              ┌─────────────────┐
      │              │      Task       │
      │              │   (Long id)     │
      └──────────────►   - projectId   │
        assigned     │   - tenantId    │
                     │   - status      │
                     │   - version     │
                     └─────────────────┘
```

## Entities

### 1. Tenant Entity

**Purpose:** Root entity for multi-tenant isolation. Each tenant represents a separate organization.

**Key Features:**
- UUID primary key (better security, prevents enumeration)
- Plan-based subscription model
- Cascading relationships to users and projects
- Automatic timestamps

**Fields:**
```java
- id: UUID (Primary Key)
- name: String (Organization name)
- plan: TenantPlan (FREE, STARTER, PROFESSIONAL, ENTERPRISE)
- status: TenantStatus (ACTIVE, SUSPENDED, TRIAL, EXPIRED)
- users: Set<User> (One-to-Many)
- projects: Set<Project> (One-to-Many)
- createdAt: LocalDateTime (Auto-generated)
- updatedAt: LocalDateTime (Auto-updated)
```

**Relationships:**
- One-to-Many with User (cascade ALL, orphan removal)
- One-to-Many with Project (cascade ALL, orphan removal)

**Indexes:**
- `idx_tenant_name` on name
- `idx_tenant_plan` on plan

**Helper Methods:**
- `addUser(User)` - Maintains bidirectional relationship
- `removeUser(User)` - Safely removes user
- `addProject(Project)` - Adds project to tenant
- `removeProject(Project)` - Removes project from tenant

---

### 2. User Entity

**Purpose:** Represents users within a tenant with role-based access control.

**Key Features:**
- Long primary key (performance)
- Email-based authentication
- Role enum (TENANT_ADMIN, TENANT_USER)
- Belongs to exactly one tenant
- Tracks created projects and assigned tasks

**Fields:**
```java
- id: Long (Primary Key)
- tenant: Tenant (Many-to-One, LAZY)
- email: String (Unique, indexed)
- password: String (Hashed)
- role: UserRole (TENANT_ADMIN, TENANT_USER)
- status: UserStatus (ACTIVE, INACTIVE, SUSPENDED)
- firstName: String
- lastName: String
- createdProjects: Set<Project> (One-to-Many)
- assignedTasks: Set<Task> (One-to-Many)
- lastLoginAt: LocalDateTime
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

**Relationships:**
- Many-to-One with Tenant (required, LAZY)
- One-to-Many with Project (as creator)
- One-to-Many with Task (as assignee)

**Indexes:**
- `idx_user_email` on email
- `idx_user_tenant` on tenant_id
- `idx_user_role` on role

**Unique Constraints:**
- `uk_user_email` on email (global uniqueness)

**Transient Methods:**
- `getTenantId()` - Returns tenant UUID
- `getFullName()` - Returns formatted name
- `isAdmin()` - Checks if user is admin

---

### 3. Project Entity

**Purpose:** Represents projects within a tenant, created by users.

**Key Features:**
- Belongs to exactly one tenant
- Has a creator (User)
- Contains multiple tasks
- Project key for task references
- Status tracking

**Fields:**
```java
- id: Long (Primary Key)
- tenant: Tenant (Many-to-One, LAZY, required)
- createdBy: User (Many-to-One, LAZY, required)
- name: String
- description: String
- projectKey: String (e.g., "PROJ", "WEB")
- status: ProjectStatus (ACTIVE, ON_HOLD, COMPLETED, ARCHIVED, CANCELLED)
- tasks: Set<Task> (One-to-Many)
- startDate: LocalDateTime
- endDate: LocalDateTime
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

**Relationships:**
- Many-to-One with Tenant (required, LAZY)
- Many-to-One with User as creator (required, LAZY)
- One-to-Many with Task (cascade ALL, orphan removal)

**Indexes:**
- `idx_project_tenant` on tenant_id
- `idx_project_created_by` on created_by_id
- `idx_project_status` on status
- `idx_project_name` on name

**Transient Methods:**
- `getTenantId()` - Returns tenant UUID
- `getCreatedById()` - Returns creator user ID
- `getTaskCount()` - Returns number of tasks
- `isActive()` - Checks if project is active

**Helper Methods:**
- `addTask(Task)` - Adds task to project
- `removeTask(Task)` - Removes task from project

---

### 4. Task Entity

**Purpose:** Represents individual tasks within projects with optimistic locking.

**Key Features:**
- Belongs to a project and tenant
- Can be assigned to a user
- **Optimistic locking with @Version**
- Status and priority tracking
- Time tracking (estimated vs actual)

**Fields:**
```java
- id: Long (Primary Key)
- project: Project (Many-to-One, LAZY, required)
- tenant: Tenant (Many-to-One, LAZY, required, denormalized)
- assignedTo: User (Many-to-One, LAZY, optional)
- title: String
- description: String
- status: TaskStatus (TODO, IN_PROGRESS, IN_REVIEW, BLOCKED, COMPLETED, CANCELLED)
- priority: TaskPriority (LOW, MEDIUM, HIGH, URGENT)
- dueDate: LocalDateTime
- estimatedHours: Integer
- actualHours: Integer
- version: Long (Optimistic locking)
- completedAt: LocalDateTime
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

**Relationships:**
- Many-to-One with Project (required, LAZY)
- Many-to-One with Tenant (required, LAZY, denormalized)
- Many-to-One with User as assignee (optional, LAZY)

**Indexes:**
- `idx_task_project` on project_id
- `idx_task_tenant` on tenant_id
- `idx_task_assigned_to` on assigned_to_id
- `idx_task_status` on status
- `idx_task_priority` on priority

**Optimistic Locking:**
```java
@Version
@Column(name = "version", nullable = false)
private Long version = 0L;
```

**How Optimistic Locking Works:**
1. Entity is loaded with current version (e.g., version = 1)
2. User modifies entity
3. On save, JPA checks if version in DB matches loaded version
4. If match: Update succeeds, version incremented (version = 2)
5. If mismatch: `OptimisticLockException` thrown (someone else updated it)

**Transient Methods:**
- `getTenantId()` - Returns tenant UUID
- `getProjectId()` - Returns project ID
- `getAssignedToId()` - Returns assignee ID
- `isCompleted()` - Checks if task is completed
- `isOverdue()` - Checks if task is overdue

**Business Methods:**
- `markAsCompleted()` - Sets status and completion timestamp

**Lifecycle Callbacks:**
- `@PrePersist onCreate()` - Ensures tenant is set from project

---

## Key Design Decisions

### 1. Multi-Tenant Isolation

**Tenant ID in Task:**
```java
// Denormalized for performance
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

**Why denormalize tenant_id in Task?**
- **Performance**: Direct index on tenant_id for fast filtering
- **Security**: Easier to enforce tenant isolation in queries
- **Queries**: Simpler WHERE clauses without joins

**Example Query:**
```sql
-- Without denormalization (requires join)
SELECT t.* FROM tasks t
JOIN projects p ON t.project_id = p.id
WHERE p.tenant_id = ?

-- With denormalization (direct filter)
SELECT * FROM tasks WHERE tenant_id = ?
```

### 2. Primary Key Strategies

**Tenant: UUID**
- Prevents enumeration attacks
- Works well in distributed systems
- No sequential guessing

**User, Project, Task: Long (IDENTITY)**
- Better performance for high-volume operations
- Smaller index size
- Simpler joins
- Sequential IDs acceptable within tenant context

### 3. Fetch Strategies

**All relationships use LAZY loading:**
```java
@ManyToOne(fetch = FetchType.LAZY)
```

**Why LAZY?**
- Prevents N+1 query problems
- Loads data only when needed
- Better performance
- Explicit control over loading

**When to use EAGER?**
- Small, frequently accessed relationships
- When you always need the related data
- Use sparingly and with caution

### 4. Cascade Operations

**Tenant → User/Project:**
```java
@OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
```
- Deleting tenant deletes all users and projects
- Removing from collection deletes entity

**Project → Task:**
```java
@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
```
- Deleting project deletes all tasks
- Maintains referential integrity

### 5. Optimistic Locking

**Why use @Version on Task?**
- Tasks are frequently updated by multiple users
- Prevents lost updates in concurrent scenarios
- No database locks needed (better performance)
- Automatic version management

**Example Scenario:**
```
Time  User A                    User B
----  ----------------------    ----------------------
T1    Load task (version=1)     
T2                              Load task (version=1)
T3    Update task               
T4    Save (version→2) ✓        
T5                              Update task
T6                              Save (version=1≠2) ✗
                                OptimisticLockException
```

**Handling OptimisticLockException:**
```java
try {
    taskRepository.save(task);
} catch (OptimisticLockException e) {
    // Reload entity and retry
    // Or inform user to refresh and try again
}
```

### 6. Indexes

**Why these indexes?**

**Tenant Indexes:**
- `name`: Search tenants by name
- `plan`: Filter by subscription plan

**User Indexes:**
- `email`: Login queries
- `tenant_id`: List users by tenant
- `role`: Filter by role

**Project Indexes:**
- `tenant_id`: Multi-tenant isolation
- `created_by_id`: Find user's projects
- `status`: Filter by status
- `name`: Search projects

**Task Indexes:**
- `project_id`: List project tasks
- `tenant_id`: Multi-tenant isolation
- `assigned_to_id`: Find user's tasks
- `status`: Filter by status
- `priority`: Sort by priority

### 7. Enums

**Why String Enums?**
```java
@Enumerated(EnumType.STRING)
```

**STRING vs ORDINAL:**
- STRING: Stores "TENANT_ADMIN" (readable, reorderable)
- ORDINAL: Stores 0, 1, 2 (compact, but fragile)

**Benefits of STRING:**
- Database values are human-readable
- Can reorder enum values safely
- Can add new values anywhere
- Easier debugging

**Drawback:**
- Slightly more storage space (negligible)

---

## Usage Examples

### Creating Entities

```java
// Create tenant
Tenant tenant = Tenant.builder()
    .name("Acme Corp")
    .plan(Tenant.TenantPlan.PROFESSIONAL)
    .build();
tenantRepository.save(tenant);

// Create user
User user = User.builder()
    .tenant(tenant)
    .email("admin@acme.com")
    .password(passwordEncoder.encode("password"))
    .role(User.UserRole.TENANT_ADMIN)
    .firstName("John")
    .lastName("Doe")
    .build();
userRepository.save(user);

// Create project
Project project = Project.builder()
    .tenant(tenant)
    .createdBy(user)
    .name("Website Redesign")
    .projectKey("WEB")
    .status(Project.ProjectStatus.ACTIVE)
    .build();
projectRepository.save(project);

// Create task
Task task = Task.builder()
    .project(project)
    .tenant(tenant)  // Denormalized
    .title("Design homepage")
    .status(Task.TaskStatus.TODO)
    .priority(Task.TaskPriority.HIGH)
    .estimatedHours(8)
    .build();
taskRepository.save(task);
```

### Querying with Tenant Isolation

```java
// Find all users in a tenant
List<User> users = userRepository.findByTenantId(tenantId);

// Find all projects in a tenant
List<Project> projects = projectRepository.findByTenantId(tenantId);

// Find all tasks in a tenant
List<Task> tasks = taskRepository.findByTenantId(tenantId);

// Find user's assigned tasks
List<Task> assignedTasks = taskRepository.findByAssignedToId(userId);
```

### Handling Optimistic Locking

```java
@Service
@Transactional
public class TaskService {
    
    public Task updateTask(Long taskId, TaskUpdateDto dto) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Task not found"));
        
        // Update fields
        task.setTitle(dto.getTitle());
        task.setStatus(dto.getStatus());
        
        try {
            return taskRepository.save(task);
        } catch (OptimisticLockException e) {
            // Task was modified by another user
            throw new ConcurrentModificationException(
                "Task was modified by another user. Please refresh and try again."
            );
        }
    }
}
```

### Using Helper Methods

```java
// Add user to tenant (maintains bidirectional relationship)
tenant.addUser(user);

// Add task to project
project.addTask(task);

// Mark task as completed
task.markAsCompleted();

// Check if task is overdue
if (task.isOverdue()) {
    // Send notification
}
```

---

## Database Schema

### Generated DDL (PostgreSQL)

```sql
-- Tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_tenant_name ON tenants(name);
CREATE INDEX idx_tenant_plan ON tenants(plan);

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_tenant ON users(tenant_id);
CREATE INDEX idx_user_role ON users(role);
CREATE UNIQUE INDEX uk_user_email ON users(email);

-- Projects table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    created_by_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    project_key VARCHAR(10),
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_project_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_project_created_by FOREIGN KEY (created_by_id) REFERENCES users(id)
);

CREATE INDEX idx_project_tenant ON projects(tenant_id);
CREATE INDEX idx_project_created_by ON projects(created_by_id);
CREATE INDEX idx_project_status ON projects(status);
CREATE INDEX idx_project_name ON projects(name);

-- Tasks table
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    assigned_to_id BIGINT,
    title VARCHAR(500) NOT NULL,
    description VARCHAR(5000),
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    due_date TIMESTAMP,
    estimated_hours INTEGER,
    actual_hours INTEGER,
    version BIGINT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_task_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_task_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users(id)
);

CREATE INDEX idx_task_project ON tasks(project_id);
CREATE INDEX idx_task_tenant ON tasks(tenant_id);
CREATE INDEX idx_task_assigned_to ON tasks(assigned_to_id);
CREATE INDEX idx_task_status ON tasks(status);
CREATE INDEX idx_task_priority ON tasks(priority);
```

---

## Best Practices

### 1. Always Filter by Tenant ID
```java
// Good: Tenant-scoped query
List<Task> tasks = taskRepository.findByTenantId(tenantId);

// Bad: Could leak data across tenants
List<Task> tasks = taskRepository.findAll();
```

### 2. Use Transient Methods
```java
// Good: Use transient method
UUID tenantId = task.getTenantId();

// Avoid: Direct navigation (might trigger lazy load)
UUID tenantId = task.getTenant().getId();
```

### 3. Handle OptimisticLockException
```java
// Always catch and handle
try {
    taskRepository.save(task);
} catch (OptimisticLockException e) {
    // Inform user and provide resolution
}
```

### 4. Use Helper Methods
```java
// Good: Maintains bidirectional relationship
tenant.addUser(user);

// Bad: Only sets one side
user.setTenant(tenant);
```

### 5. Validate Tenant Consistency
```java
// In service layer
if (!task.getTenantId().equals(currentTenantId)) {
    throw new SecurityException("Access denied");
}
```

---

## Summary

This multi-tenant entity model provides:

✅ **Proper tenant isolation** with denormalized tenant_id  
✅ **Optimistic locking** for concurrent updates  
✅ **Efficient indexing** for common queries  
✅ **Bidirectional relationships** with helper methods  
✅ **Automatic timestamps** with Hibernate annotations  
✅ **Type-safe enums** for status and roles  
✅ **Lazy loading** for performance  
✅ **Cascade operations** for data integrity  
✅ **Comprehensive documentation** in code  

The entities are production-ready and follow JPA best practices!

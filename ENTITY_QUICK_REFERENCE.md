# JPA Entities Quick Reference

## Entity Overview

```
Tenant (UUID) → User (Long) → Task (Long)
       ↓           ↓
   Project (Long) ─┘
```

## Tenant Entity

```java
@Entity
@Table(name = "tenants")
public class Tenant {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private TenantPlan plan;  // FREE, STARTER, PROFESSIONAL, ENTERPRISE
    
    @OneToMany(mappedBy = "tenant", cascade = ALL, orphanRemoval = true)
    private Set<User> users;
    
    @OneToMany(mappedBy = "tenant", cascade = ALL, orphanRemoval = true)
    private Set<Project> projects;
}
```

**Key Points:**
- UUID primary key
- Root of tenant hierarchy
- Cascades to users and projects

## User Entity

```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String password;  // Hashed
    
    @Enumerated(EnumType.STRING)
    private UserRole role;  // TENANT_ADMIN, TENANT_USER
    
    @OneToMany(mappedBy = "createdBy")
    private Set<Project> createdProjects;
    
    @OneToMany(mappedBy = "assignedTo")
    private Set<Task> assignedTasks;
}
```

**Key Points:**
- Belongs to one tenant
- Email is globally unique
- Role-based access control

## Project Entity

```java
@Entity
@Table(name = "projects")
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    
    private String name;
    private String projectKey;
    
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;  // ACTIVE, ON_HOLD, COMPLETED, etc.
    
    @OneToMany(mappedBy = "project", cascade = ALL, orphanRemoval = true)
    private Set<Task> tasks;
}
```

**Key Points:**
- Belongs to tenant and creator
- Contains multiple tasks
- Project key for references

## Task Entity

```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;  // Denormalized for performance
    
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;  // Optional
    
    private String title;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;  // TODO, IN_PROGRESS, COMPLETED, etc.
    
    @Enumerated(EnumType.STRING)
    private TaskPriority priority;  // LOW, MEDIUM, HIGH, URGENT
    
    @Version  // Optimistic locking
    private Long version = 0L;
}
```

**Key Points:**
- Belongs to project and tenant
- **Optimistic locking with @Version**
- Can be assigned to user

## Enums

### TenantPlan
```java
FREE, STARTER, PROFESSIONAL, ENTERPRISE
```

### UserRole
```java
TENANT_ADMIN,  // Full access
TENANT_USER    // Limited access
```

### ProjectStatus
```java
ACTIVE, ON_HOLD, COMPLETED, ARCHIVED, CANCELLED
```

### TaskStatus
```java
TODO, IN_PROGRESS, IN_REVIEW, BLOCKED, COMPLETED, CANCELLED
```

### TaskPriority
```java
LOW, MEDIUM, HIGH, URGENT
```

## Key Annotations

### Entity Level
```java
@Entity                    // Marks as JPA entity
@Table(name = "...")      // Table name and indexes
@Data                     // Lombok: getters, setters, etc.
@Builder                  // Lombok: builder pattern
@NoArgsConstructor        // Lombok: no-arg constructor
@AllArgsConstructor       // Lombok: all-args constructor
```

### Field Level
```java
@Id                       // Primary key
@GeneratedValue           // Auto-generate value
@Column                   // Column details
@Enumerated(EnumType.STRING)  // Store enum as string
@Version                  // Optimistic locking
@CreationTimestamp        // Auto-set on create
@UpdateTimestamp          // Auto-update on modify
@Transient                // Not persisted
```

### Relationship Annotations
```java
@ManyToOne(fetch = LAZY)  // Many entities to one
@OneToMany(mappedBy = "field")  // One to many
@JoinColumn(name = "...")  // Foreign key column
```

## Optimistic Locking

### How It Works
```java
@Version
private Long version = 0L;

// 1. Load: version = 1
// 2. Modify entity
// 3. Save: Check if DB version = 1
// 4. If match: Update and increment (version = 2)
// 5. If mismatch: Throw OptimisticLockException
```

### Handling Conflicts
```java
try {
    taskRepository.save(task);
} catch (OptimisticLockException e) {
    // Reload and retry, or inform user
}
```

## Common Queries

### Find by Tenant
```java
// Users in tenant
userRepository.findByTenantId(tenantId);

// Projects in tenant
projectRepository.findByTenantId(tenantId);

// Tasks in tenant
taskRepository.findByTenantId(tenantId);
```

### Find by User
```java
// Projects created by user
projectRepository.findByCreatedById(userId);

// Tasks assigned to user
taskRepository.findByAssignedToId(userId);
```

### Find by Status
```java
// Active projects
projectRepository.findByStatus(ProjectStatus.ACTIVE);

// TODO tasks
taskRepository.findByStatus(TaskStatus.TODO);
```

## Helper Methods

### Tenant
```java
tenant.addUser(user);           // Add user to tenant
tenant.removeUser(user);        // Remove user
tenant.addProject(project);     // Add project
tenant.removeProject(project);  // Remove project
```

### Project
```java
project.addTask(task);          // Add task to project
project.removeTask(task);       // Remove task
project.getTaskCount();         // Count tasks
project.isActive();             // Check if active
```

### Task
```java
task.markAsCompleted();         // Mark as done
task.isCompleted();             // Check if done
task.isOverdue();               // Check if overdue
task.getTenantId();             // Get tenant UUID
```

## Indexes

### Tenant
- `idx_tenant_name` on name
- `idx_tenant_plan` on plan

### User
- `idx_user_email` on email
- `idx_user_tenant` on tenant_id
- `idx_user_role` on role
- `uk_user_email` unique on email

### Project
- `idx_project_tenant` on tenant_id
- `idx_project_created_by` on created_by_id
- `idx_project_status` on status
- `idx_project_name` on name

### Task
- `idx_task_project` on project_id
- `idx_task_tenant` on tenant_id
- `idx_task_assigned_to` on assigned_to_id
- `idx_task_status` on status
- `idx_task_priority` on priority

## Usage Example

```java
// Create tenant
Tenant tenant = Tenant.builder()
    .name("Acme Corp")
    .plan(TenantPlan.PROFESSIONAL)
    .build();

// Create user
User user = User.builder()
    .tenant(tenant)
    .email("admin@acme.com")
    .password(encoded)
    .role(UserRole.TENANT_ADMIN)
    .build();

// Create project
Project project = Project.builder()
    .tenant(tenant)
    .createdBy(user)
    .name("Website")
    .projectKey("WEB")
    .build();

// Create task
Task task = Task.builder()
    .project(project)
    .tenant(tenant)
    .title("Design homepage")
    .status(TaskStatus.TODO)
    .priority(TaskPriority.HIGH)
    .build();
```

## Best Practices

✅ Always filter by tenant ID  
✅ Use LAZY fetch for relationships  
✅ Handle OptimisticLockException  
✅ Use helper methods for relationships  
✅ Validate tenant consistency  
✅ Use transient methods for IDs  
✅ Store enums as STRING  
✅ Index foreign keys  

## Key Design Features

🔐 **Multi-tenant isolation** - Tenant ID in all entities  
🔒 **Optimistic locking** - @Version on Task  
⚡ **Performance** - Denormalized tenant_id, proper indexes  
🔗 **Relationships** - Bidirectional with helper methods  
⏰ **Timestamps** - Automatic with Hibernate  
🎯 **Type safety** - Enums for status/roles  
📊 **Lazy loading** - Better performance  
🔄 **Cascading** - Automatic cleanup  

---

**All entities are production-ready with proper JPA annotations!** ✅

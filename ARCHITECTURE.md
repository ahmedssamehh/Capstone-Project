# Architecture Documentation

## Overview

WorkHub follows a **Clean Architecture** approach with clear separation of concerns and dependency inversion principles.

## Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│                      (Controllers)                           │
│  - REST API endpoints                                        │
│  - Request/Response handling                                 │
│  - Input validation                                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│                    (Service Interfaces)                      │
│  - Business logic contracts                                  │
│  - Use case definitions                                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  Business Logic Layer                        │
│                 (Service Implementations)                    │
│  - Business rules                                            │
│  - Domain logic                                              │
│  - Transaction management                                    │
│  - Tenant isolation enforcement                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Access Layer                       │
│                      (Repositories)                          │
│  - Database operations                                       │
│  - Query methods                                             │
│  - Spring Data JPA                                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│                        (Entities)                            │
│  - Domain models                                             │
│  - Business entities                                         │
│  - JPA mappings                                              │
└─────────────────────────────────────────────────────────────┘
```

## Cross-Cutting Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Layer                            │
│  - JWT Authentication                                        │
│  - Tenant Context Management                                 │
│  - Authorization                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  Exception Handling                          │
│  - Global exception handler                                  │
│  - Standardized error responses                              │
│  - Validation error handling                                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Configuration                             │
│  - Application configuration                                 │
│  - JPA configuration                                         │
│  - Security configuration                                    │
└─────────────────────────────────────────────────────────────┘
```

## Multi-Tenancy Architecture

### Tenant Isolation Strategy

This application uses **Discriminator Column** approach for multi-tenancy:

```
┌──────────────────┐
│  HTTP Request    │
│  X-Tenant-ID     │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────┐
│  TenantContextFilter     │
│  - Extracts tenant ID    │
│  - Sets TenantContext    │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│    TenantContext         │
│  - ThreadLocal storage   │
│  - Current tenant ID     │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│   Service Layer          │
│  - Validates tenant      │
│  - Enforces isolation    │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│   Repository Layer       │
│  - Filters by tenant     │
│  - Query constraints     │
└──────────────────────────┘
```

### Data Flow

1. **Request arrives** with `X-Tenant-ID` header
2. **TenantContextFilter** extracts and validates tenant ID
3. **TenantContext** stores tenant ID in ThreadLocal
4. **Service layer** validates tenant access for all operations
5. **Repository layer** automatically filters queries by tenant
6. **Response** contains only tenant-specific data

## Entity Relationships

```
┌──────────────┐
│    Tenant    │
│              │
│  - id (UUID) │
│  - name      │
│  - slug      │
│  - status    │
└──────┬───────┘
       │
       │ 1:N
       │
       ├─────────────────────────────┐
       │                             │
       ▼                             ▼
┌──────────────┐              ┌──────────────┐
│     User     │              │   Project    │
│              │              │              │
│  - id        │              │  - id        │
│  - email     │◄─────────────┤  - name      │
│  - role      │    owner     │  - key       │
│  - status    │              │  - status    │
└──────┬───────┘              └──────┬───────┘
       │                             │
       │ 1:N                         │ 1:N
       │ assigned                    │
       │                             │
       │        ┌──────────────┐     │
       └────────►     Task     │◄────┘
                │              │
                │  - id        │
                │  - title     │
                │  - status    │
                │  - priority  │
                └──────────────┘
```

## Security Flow

### Authentication Flow

```
┌──────────────┐
│   Client     │
└──────┬───────┘
       │
       │ POST /api/auth/login
       │ { email, password }
       │
       ▼
┌──────────────────────┐
│  Authentication      │
│  Controller          │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│  Authentication      │
│  Service             │
│  - Validate creds    │
│  - Generate JWT      │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│    JWT Service       │
│  - Create token      │
│  - Sign with secret  │
└──────┬───────────────┘
       │
       │ JWT Token
       │
       ▼
┌──────────────┐
│   Client     │
│  Stores JWT  │
└──────────────┘
```

### Authorization Flow

```
┌──────────────┐
│   Client     │
│  + JWT Token │
└──────┬───────┘
       │
       │ GET /api/projects
       │ Authorization: Bearer {JWT}
       │ X-Tenant-ID: {tenant-id}
       │
       ▼
┌──────────────────────────┐
│  JwtAuthenticationFilter │
│  - Extract JWT           │
│  - Validate token        │
│  - Set authentication    │
└──────┬───────────────────┘
       │
       ▼
┌──────────────────────────┐
│  TenantContextFilter     │
│  - Extract tenant ID     │
│  - Validate access       │
└──────┬───────────────────┘
       │
       ▼
┌──────────────────────────┐
│  Security Context        │
│  - User authenticated    │
│  - Tenant identified     │
└──────┬───────────────────┘
       │
       ▼
┌──────────────────────────┐
│  Controller              │
│  - Process request       │
└──────┬───────────────────┘
       │
       ▼
┌──────────────────────────┐
│  Service Layer           │
│  - Validate tenant       │
│  - Execute business logic│
└──────────────────────────┘
```

## Design Patterns Used

### 1. Repository Pattern
- Abstracts data access logic
- Provides clean interface for data operations
- Spring Data JPA implementation

### 2. Service Layer Pattern
- Encapsulates business logic
- Provides transaction boundaries
- Interface-based design for flexibility

### 3. DTO Pattern
- Separates API contracts from domain models
- Request/Response objects
- Prevents over-exposure of domain entities

### 4. Builder Pattern
- Used with Lombok @Builder
- Fluent entity creation
- Immutable object construction

### 5. Strategy Pattern
- Different tenant isolation strategies possible
- Pluggable authentication mechanisms

### 6. Filter Chain Pattern
- Security filters
- Tenant context establishment
- Request/Response processing

## Key Design Decisions

### 1. UUID for Tenant IDs
- **Why**: Prevents enumeration attacks
- **Why**: Globally unique identifiers
- **Why**: Easier for distributed systems

### 2. ThreadLocal for Tenant Context
- **Why**: Request-scoped tenant information
- **Why**: No need to pass tenant ID through all layers
- **Why**: Clean separation of concerns

### 3. Separate DTOs
- **Why**: API stability
- **Why**: Validation at API boundary
- **Why**: Prevents accidental exposure of sensitive data

### 4. Service Interfaces
- **Why**: Testability
- **Why**: Flexibility to change implementations
- **Why**: Clear contracts

### 5. Global Exception Handler
- **Why**: Consistent error responses
- **Why**: Centralized error handling logic
- **Why**: Better client experience

## Scalability Considerations

### Horizontal Scaling
- Stateless design (JWT tokens)
- No server-side session storage
- Can run multiple instances

### Database Optimization
- Proper indexing on tenant_id
- Composite indexes for common queries
- Connection pooling

### Caching Strategy (Future)
- Redis for session data
- Cache tenant configurations
- Query result caching

### Performance
- Lazy loading for relationships
- Pagination for large datasets
- Optimized queries with proper joins

## Testing Strategy

### Unit Tests
- Service layer logic
- Business rule validation
- Utility functions

### Integration Tests
- Repository layer
- Database operations
- Transaction management

### API Tests
- Controller endpoints
- Request/Response validation
- Security rules

### Security Tests
- Authentication flows
- Authorization rules
- Tenant isolation

## Deployment Architecture

```
┌─────────────────────────────────────────────────┐
│              Load Balancer                      │
└────────────────┬────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
┌──────────────┐  ┌──────────────┐
│  App Server  │  │  App Server  │
│  Instance 1  │  │  Instance 2  │
└──────┬───────┘  └──────┬───────┘
       │                 │
       └────────┬────────┘
                │
                ▼
        ┌──────────────┐
        │  PostgreSQL  │
        │   Database   │
        └──────────────┘
```

## Monitoring and Observability

### Logging
- Structured logging with SLF4J
- Request/Response logging
- Error tracking
- Audit trails

### Metrics (Future)
- API response times
- Database query performance
- Error rates
- Tenant usage statistics

### Health Checks
- Database connectivity
- Application health endpoint
- Readiness/Liveness probes

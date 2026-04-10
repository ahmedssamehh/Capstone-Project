package com.example.app.controller;

import com.example.app.model.User;
import com.example.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller - REST API for User operations
 * 
 * This demonstrates:
 * 1. RESTful API design
 * 2. Request/Response handling
 * 3. Input validation
 * 4. HTTP status codes
 * 5. Exception handling
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * CREATE - POST /api/users
     * 
     * @Valid triggers Bean Validation on User object
     * - Validates @NotBlank, @Email, @Size, etc.
     * - Returns 400 Bad Request if validation fails
     * - Error details in response body
     * 
     * @RequestBody deserializes JSON to User object
     * 
     * Example request:
     * POST /api/users
     * {
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "age": 30
     * }
     * 
     * Test with:
     * curl -X POST http://localhost:8080/api/users \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"John Doe","email":"john@example.com","age":30}'
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        log.info("REST request to create user: {}", user.getEmail());
        
        User createdUser = userService.createUser(user);
        
        // Return 201 Created with created resource
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    /**
     * READ - GET /api/users/{id}
     * 
     * @PathVariable extracts {id} from URL path
     * 
     * Example: GET /api/users/1
     * 
     * Test with:
     * curl http://localhost:8080/api/users/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("REST request to get user by ID: {}", id);
        
        User user = userService.getUserById(id);
        
        // Return 200 OK with user data
        return ResponseEntity.ok(user);
    }

    /**
     * READ ALL - GET /api/users
     * 
     * Optional query parameter: active
     * - If provided: filter by active status
     * - If not provided: return all users
     * 
     * Example: GET /api/users
     * Example: GET /api/users?active=true
     * 
     * Test with:
     * curl http://localhost:8080/api/users
     * curl "http://localhost:8080/api/users?active=true"
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(required = false) Boolean active) {
        log.info("REST request to get all users, active filter: {}", active);
        
        List<User> users;
        if (active != null && active) {
            users = userService.getActiveUsers();
        } else {
            users = userService.getAllUsers();
        }
        
        return ResponseEntity.ok(users);
    }

    /**
     * SEARCH - GET /api/users/search
     * 
     * Query parameter: name (required)
     * 
     * Example: GET /api/users/search?name=John
     * 
     * Test with:
     * curl "http://localhost:8080/api/users/search?name=John"
     */
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam String name) {
        log.info("REST request to search users by name: {}", name);
        
        List<User> users = userService.searchUsersByName(name);
        
        return ResponseEntity.ok(users);
    }

    /**
     * READ BY EMAIL - GET /api/users/email/{email}
     * 
     * Example: GET /api/users/email/john@example.com
     * 
     * Test with:
     * curl http://localhost:8080/api/users/email/john@example.com
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        log.info("REST request to get user by email: {}", email);
        
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * UPDATE - PUT /api/users/{id}
     * 
     * PUT is for full update (replace entire resource)
     * PATCH would be for partial update
     * 
     * Example request:
     * PUT /api/users/1
     * {
     *   "name": "John Updated",
     *   "email": "john.updated@example.com",
     *   "age": 31
     * }
     * 
     * Test with:
     * curl -X PUT http://localhost:8080/api/users/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"John Updated","email":"john.updated@example.com","age":31}'
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user) {
        log.info("REST request to update user: {}", id);
        
        User updatedUser = userService.updateUser(id, user);
        
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * DEACTIVATE - POST /api/users/{id}/deactivate
     * 
     * Soft delete - marks user as inactive
     * 
     * Test with:
     * curl -X POST http://localhost:8080/api/users/1/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        log.info("REST request to deactivate user: {}", id);
        
        userService.deactivateUser(id);
        
        // Return 204 No Content (success, no body)
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE - DELETE /api/users/{id}
     * 
     * Hard delete - permanently removes user
     * 
     * Test with:
     * curl -X DELETE http://localhost:8080/api/users/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("REST request to delete user: {}", id);
        
        userService.deleteUser(id);
        
        // Return 204 No Content
        return ResponseEntity.noContent().build();
    }

    /**
     * COUNT - GET /api/users/count
     * 
     * Returns total number of users
     * 
     * Test with:
     * curl http://localhost:8080/api/users/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getUserCount() {
        log.info("REST request to get user count");
        
        long count = userService.getUserCount();
        
        return ResponseEntity.ok(count);
    }

    /**
     * Exception handler for this controller
     * 
     * Catches IllegalArgumentException thrown by service layer
     * Returns 400 Bad Request with error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Validation error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
}

/**
 * REST API CONVENTIONS
 * 
 * HTTP Methods:
 * - GET: Retrieve resource(s) - Idempotent, Safe
 * - POST: Create new resource - Not idempotent
 * - PUT: Update entire resource - Idempotent
 * - PATCH: Partial update - Not necessarily idempotent
 * - DELETE: Remove resource - Idempotent
 * 
 * Status Codes:
 * - 200 OK: Success (GET, PUT, PATCH)
 * - 201 Created: Resource created (POST)
 * - 204 No Content: Success with no body (DELETE)
 * - 400 Bad Request: Invalid input
 * - 404 Not Found: Resource doesn't exist
 * - 500 Internal Server Error: Server error
 * 
 * URL Patterns:
 * - Collection: /api/users (GET all, POST new)
 * - Resource: /api/users/{id} (GET, PUT, DELETE)
 * - Action: /api/users/{id}/deactivate (POST)
 * - Search: /api/users/search?name=John (GET)
 * 
 * Request/Response:
 * - Content-Type: application/json
 * - Accept: application/json
 * - Body: JSON format
 * 
 * WHAT SPRING MVC DOES
 * 
 * 1. Request arrives: POST /api/users
 * 2. DispatcherServlet receives request
 * 3. Finds matching @RequestMapping
 * 4. Deserializes JSON to User object (@RequestBody)
 * 5. Validates object (@Valid)
 * 6. Calls controller method
 * 7. Controller calls service
 * 8. Service calls repository
 * 9. Repository executes SQL
 * 10. Result flows back up
 * 11. Controller returns ResponseEntity
 * 12. Spring serializes to JSON
 * 13. Sets HTTP status and headers
 * 14. Sends response to client
 * 
 * All of this is handled automatically by Spring!
 */

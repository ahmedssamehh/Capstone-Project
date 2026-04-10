package com.example.app.service;

import com.example.app.model.User;
import com.example.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * User Service - Business Logic Layer
 * 
 * This demonstrates:
 * 1. Service layer pattern
 * 2. Transaction management
 * 3. Business logic separation
 * 4. Dependency injection
 */

/**
 * @Service - Marks this as a service component
 * - Spring will detect and register as a bean
 * - Semantic annotation (same as @Component but clearer intent)
 * - Enables transaction management
 * - Allows for future AOP enhancements
 */
@Service
/**
 * Lombok @RequiredArgsConstructor
 * - Generates constructor with all 'final' fields
 * - Used for constructor-based dependency injection
 * - Replaces @Autowired on constructor
 * 
 * Generated code:
 * public UserService(UserRepository userRepository) {
 *     this.userRepository = userRepository;
 * }
 */
@RequiredArgsConstructor
/**
 * Lombok @Slf4j
 * - Generates a logger field: private static final Logger log
 * - Uses SLF4J (Simple Logging Facade for Java)
 * - Allows: log.info(), log.error(), log.debug(), etc.
 * 
 * Generated code:
 * private static final org.slf4j.Logger log = 
 *     org.slf4j.LoggerFactory.getLogger(UserService.class);
 */
@Slf4j
/**
 * @Transactional - Class-level transaction management
 * - All public methods run in a transaction
 * - Automatic commit on success
 * - Automatic rollback on exception
 * - Can be overridden at method level
 * 
 * What it does:
 * 1. Opens database transaction before method
 * 2. Executes method
 * 3. Commits if successful
 * 4. Rolls back if exception thrown
 * 5. Closes transaction
 */
@Transactional
public class UserService {

    /**
     * Repository dependency
     * - Injected via constructor (see @RequiredArgsConstructor)
     * - final ensures immutability
     * - No @Autowired needed (constructor injection is preferred)
     */
    private final UserRepository userRepository;

    /**
     * Create a new user
     * 
     * Transaction behavior:
     * - Opens transaction
     * - Validates user (Bean Validation)
     * - Saves to database
     * - Commits transaction
     * - Returns saved entity with generated ID
     * 
     * @param user User to create
     * @return Saved user with generated ID
     */
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getEmail());
        
        // Business logic: Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            log.error("User with email {} already exists", user.getEmail());
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        // Save user (transaction will commit automatically)
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return savedUser;
    }

    /**
     * Get user by ID
     * 
     * @Transactional(readOnly = true)
     * - Optimizes read-only operations
     * - No need to track entity changes
     * - Better performance
     * - Some databases can optimize read-only transactions
     * 
     * @param id User ID
     * @return User if found
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", id);
                    return new IllegalArgumentException("User not found: " + id);
                });
    }

    /**
     * Get user by email
     * 
     * @param email User email
     * @return User if found
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Get all users
     * 
     * @return List of all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Get active users only
     * 
     * Business logic: Filter by active status
     * 
     * @return List of active users
     */
    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        log.debug("Fetching active users");
        return userRepository.findByActive(true);
    }

    /**
     * Search users by name
     * 
     * @param name Name to search (partial match)
     * @return List of matching users
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByName(String name) {
        log.debug("Searching users by name: {}", name);
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Update user
     * 
     * Transaction behavior:
     * 1. Fetch existing user (within transaction)
     * 2. Modify fields
     * 3. Save changes
     * 4. Commit transaction
     * 
     * Note: With @Transactional, you can just modify the entity
     * and changes will be automatically persisted (dirty checking)
     * 
     * @param id User ID
     * @param updatedUser User with updated data
     * @return Updated user
     */
    public User updateUser(Long id, User updatedUser) {
        log.info("Updating user with ID: {}", id);
        
        // Fetch existing user
        User existingUser = getUserById(id);
        
        // Business logic: Validate email uniqueness if changed
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            if (userRepository.existsByEmail(updatedUser.getEmail())) {
                log.error("Email already in use: {}", updatedUser.getEmail());
                throw new IllegalArgumentException("Email already in use: " + updatedUser.getEmail());
            }
        }
        
        // Update fields
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setAge(updatedUser.getAge());
        existingUser.setActive(updatedUser.getActive());
        
        // Save changes (or rely on dirty checking)
        User savedUser = userRepository.save(existingUser);
        log.info("User updated successfully: {}", savedUser.getId());
        
        return savedUser;
    }

    /**
     * Deactivate user (soft delete)
     * 
     * Business logic: Don't delete, just mark as inactive
     * 
     * @param id User ID
     */
    public void deactivateUser(Long id) {
        log.info("Deactivating user with ID: {}", id);
        
        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);
        
        log.info("User deactivated: {}", id);
    }

    /**
     * Delete user (hard delete)
     * 
     * Use with caution - permanently removes data
     * 
     * @param id User ID
     */
    public void deleteUser(Long id) {
        log.warn("Deleting user with ID: {}", id);
        
        // Verify user exists
        if (!userRepository.existsById(id)) {
            log.error("Cannot delete - user not found: {}", id);
            throw new IllegalArgumentException("User not found: " + id);
        }
        
        userRepository.deleteById(id);
        log.info("User deleted: {}", id);
    }

    /**
     * Get user count
     * 
     * @return Total number of users
     */
    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Get active user count
     * 
     * @return Number of active users
     */
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countByActive(true);
    }
}

/**
 * WHY SERVICE LAYER?
 * 
 * 1. Separation of Concerns
 *    - Controllers handle HTTP
 *    - Services handle business logic
 *    - Repositories handle data access
 * 
 * 2. Transaction Management
 *    - @Transactional at service layer
 *    - Multiple repository calls in one transaction
 *    - Automatic rollback on errors
 * 
 * 3. Business Logic
 *    - Validation rules
 *    - Business rules
 *    - Complex operations
 * 
 * 4. Reusability
 *    - Same service used by multiple controllers
 *    - Can be called from scheduled tasks
 *    - Can be used in batch operations
 * 
 * 5. Testing
 *    - Easy to mock repositories
 *    - Test business logic independently
 *    - No need for HTTP layer
 * 
 * TRANSACTION MANAGEMENT EXPLAINED
 * 
 * Without @Transactional:
 * - Each repository call is a separate transaction
 * - No automatic rollback
 * - Inconsistent state on errors
 * 
 * With @Transactional:
 * - All operations in one transaction
 * - Automatic commit on success
 * - Automatic rollback on exception
 * - ACID guarantees
 * 
 * Example scenario:
 * 
 * public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
 *     // Without @Transactional:
 *     // 1. Deduct from account A - SUCCESS
 *     // 2. Add to account B - FAILS
 *     // Result: Money lost!
 *     
 *     // With @Transactional:
 *     // 1. Deduct from account A
 *     // 2. Add to account B - FAILS
 *     // Result: Automatic rollback, both operations undone
 * }
 */

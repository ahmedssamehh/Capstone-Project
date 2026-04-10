package com.example.app.repository;

import com.example.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository - Data Access Layer
 * 
 * This demonstrates Spring Data JPA repository pattern
 * 
 * Key concepts:
 * 1. Interface-based repositories (no implementation needed)
 * 2. Automatic query generation from method names
 * 3. Custom queries with @Query
 * 4. Type-safe operations
 */

/**
 * @Repository - Marks this as a repository component
 * - Spring will create a proxy implementation at runtime
 * - Enables exception translation (SQLException -> DataAccessException)
 * - Optional when extending Spring Data interfaces, but good practice
 */
@Repository
/**
 * JpaRepository<User, Long>
 * - Generic interface: JpaRepository<EntityType, IdType>
 * - User: The entity this repository manages
 * - Long: The type of the entity's primary key
 * 
 * What JpaRepository provides (inherited methods):
 * - save(entity): Insert or update
 * - findById(id): Find by primary key
 * - findAll(): Get all entities
 * - deleteById(id): Delete by primary key
 * - count(): Count all entities
 * - existsById(id): Check if exists
 * ... and many more
 * 
 * Hierarchy:
 * JpaRepository extends PagingAndSortingRepository
 * PagingAndSortingRepository extends CrudRepository
 * CrudRepository extends Repository
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * DERIVED QUERY METHODS
     * 
     * Spring Data JPA automatically generates implementation
     * based on method name following naming convention:
     * 
     * Pattern: findBy + PropertyName + Operator
     * 
     * This method will generate SQL:
     * SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Find users by name (case-sensitive)
     * 
     * Generated SQL:
     * SELECT * FROM users WHERE name = ?
     */
    List<User> findByName(String name);

    /**
     * Find users by name containing string (case-insensitive)
     * 
     * Generated SQL:
     * SELECT * FROM users WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'))
     */
    List<User> findByNameContainingIgnoreCase(String name);

    /**
     * Find active users
     * 
     * Generated SQL:
     * SELECT * FROM users WHERE active = ?
     */
    List<User> findByActive(Boolean active);

    /**
     * Find users by age greater than
     * 
     * Generated SQL:
     * SELECT * FROM users WHERE age > ?
     */
    List<User> findByAgeGreaterThan(Integer age);

    /**
     * Find users by age between range
     * 
     * Generated SQL:
     * SELECT * FROM users WHERE age BETWEEN ? AND ?
     */
    List<User> findByAgeBetween(Integer minAge, Integer maxAge);

    /**
     * Find users by name and active status
     * 
     * Generated SQL:
     * SELECT * FROM users WHERE name = ? AND active = ?
     */
    List<User> findByNameAndActive(String name, Boolean active);

    /**
     * Find users by name or email
     * 
     * Generated SQL:
     * SELECT * FROM users WHERE name = ? OR email = ?
     */
    List<User> findByNameOrEmail(String name, String email);

    /**
     * Find users ordered by name
     * 
     * Generated SQL:
     * SELECT * FROM users ORDER BY name ASC
     */
    List<User> findAllByOrderByNameAsc();

    /**
     * Check if user exists by email
     * 
     * Generated SQL:
     * SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * 
     * Returns boolean instead of entity (more efficient)
     */
    boolean existsByEmail(String email);

    /**
     * Count users by active status
     * 
     * Generated SQL:
     * SELECT COUNT(*) FROM users WHERE active = ?
     */
    long countByActive(Boolean active);

    /**
     * Delete users by name
     * 
     * Generated SQL:
     * DELETE FROM users WHERE name = ?
     * 
     * Returns: number of deleted entities
     */
    long deleteByName(String name);

    /**
     * CUSTOM QUERIES WITH @Query
     * 
     * When method name queries are not enough,
     * use @Query annotation with JPQL or native SQL
     */

    /**
     * Custom JPQL query
     * 
     * JPQL (Java Persistence Query Language):
     * - Uses entity and property names (not table/column names)
     * - Database-independent
     * - Type-safe
     * 
     * :name is a named parameter
     */
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
    List<User> searchByName(@Param("name") String name);

    /**
     * Custom JPQL with multiple conditions
     */
    @Query("SELECT u FROM User u WHERE u.active = :active AND u.age >= :minAge")
    List<User> findActiveUsersAboveAge(
        @Param("active") Boolean active,
        @Param("minAge") Integer minAge
    );

    /**
     * Native SQL query
     * 
     * nativeQuery = true: Use actual SQL instead of JPQL
     * - Database-specific
     * - Use when JPQL is not sufficient
     * - Access to database-specific features
     */
    @Query(value = "SELECT * FROM users WHERE email ILIKE :email", nativeQuery = true)
    Optional<User> findByEmailIgnoreCaseNative(@Param("email") String email);

    /**
     * Count query
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();

    /**
     * Update query
     * 
     * @Modifying: Required for UPDATE/DELETE queries
     * Must be used with @Transactional in service layer
     */
    // @Modifying
    // @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
    // int updateActiveStatus(@Param("id") Long id, @Param("active") Boolean active);
}

/**
 * QUERY METHOD KEYWORDS
 * 
 * Spring Data JPA supports these keywords in method names:
 * 
 * - find...By, read...By, get...By, query...By, search...By, stream...By
 * - exists...By
 * - count...By
 * - delete...By, remove...By
 * 
 * Property expressions:
 * - And, Or
 * - Is, Equals
 * - Between
 * - LessThan, LessThanEqual
 * - GreaterThan, GreaterThanEqual
 * - After, Before
 * - IsNull, IsNotNull, NotNull
 * - Like, NotLike
 * - StartingWith, EndingWith, Containing
 * - OrderBy
 * - Not, In, NotIn
 * - True, False
 * - IgnoreCase
 * 
 * Examples:
 * - findByNameAndEmail(String name, String email)
 * - findByAgeLessThan(Integer age)
 * - findByNameStartingWith(String prefix)
 * - findByActiveTrue()
 * - findByEmailIsNotNull()
 * - findByNameIn(List<String> names)
 */

/**
 * WHAT SPRING DATA JPA DOES
 * 
 * At runtime, Spring creates a proxy implementation of this interface:
 * 
 * 1. Parses method names
 * 2. Generates appropriate queries
 * 3. Executes queries using EntityManager
 * 4. Maps results to entities
 * 5. Handles transactions
 * 6. Manages entity lifecycle
 * 
 * You write: findByEmail(String email)
 * Spring generates:
 * 
 * public Optional<User> findByEmail(String email) {
 *     TypedQuery<User> query = entityManager.createQuery(
 *         "SELECT u FROM User u WHERE u.email = :email", User.class);
 *     query.setParameter("email", email);
 *     try {
 *         return Optional.of(query.getSingleResult());
 *     } catch (NoResultException e) {
 *         return Optional.empty();
 *     }
 * }
 * 
 * This saves you from writing ~500 lines of boilerplate code!
 */

package com.example.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple REST Controller to verify setup
 * 
 * @RestController combines:
 * - @Controller: Marks this as a Spring MVC controller
 * - @ResponseBody: Automatically serializes return values to JSON
 * 
 * @RequestMapping: Base path for all endpoints in this controller
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    /**
     * Simple GET endpoint
     * 
     * @GetMapping is shorthand for @RequestMapping(method = RequestMethod.GET)
     * 
     * What happens:
     * 1. Spring MVC maps GET /api/hello to this method
     * 2. Method executes and returns String
     * 3. @RestController ensures response is written directly (not a view name)
     * 4. Jackson converts String to JSON response
     * 
     * Test with: curl http://localhost:8080/api/hello
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello from manually configured Spring Boot!";
    }

    /**
     * GET endpoint with path variable
     * 
     * @PathVariable extracts value from URL path
     * 
     * Example: GET /api/hello/John
     * Returns: "Hello, John!"
     * 
     * Test with: curl http://localhost:8080/api/hello/John
     */
    @GetMapping("/hello/{name}")
    public String helloName(@PathVariable String name) {
        return "Hello, " + name + "!";
    }

    /**
     * GET endpoint with query parameter
     * 
     * @RequestParam extracts value from query string
     * - required = false: parameter is optional
     * - defaultValue: used if parameter not provided
     * 
     * Example: GET /api/greet?name=John
     * Returns: "Greetings, John!"
     * 
     * Test with: curl "http://localhost:8080/api/greet?name=John"
     */
    @GetMapping("/greet")
    public String greet(@RequestParam(required = false, defaultValue = "Guest") String name) {
        return "Greetings, " + name + "!";
    }

    /**
     * POST endpoint with request body
     * 
     * @PostMapping is shorthand for @RequestMapping(method = RequestMethod.POST)
     * @RequestBody: Deserializes JSON request body to Java object
     * 
     * Example POST body:
     * {
     *   "message": "Test message"
     * }
     * 
     * Test with:
     * curl -X POST http://localhost:8080/api/echo \
     *   -H "Content-Type: application/json" \
     *   -d '{"message":"Test message"}'
     */
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", request.get("message"));
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "success");
        return response;
    }

    /**
     * Endpoint returning ResponseEntity for full control
     * 
     * ResponseEntity allows you to:
     * - Set HTTP status code
     * - Add custom headers
     * - Control response body
     * 
     * Test with: curl -i http://localhost:8080/api/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("application", "Manual Spring Boot Setup");
        status.put("status", "running");
        status.put("timestamp", LocalDateTime.now());
        status.put("java.version", System.getProperty("java.version"));
        status.put("spring.version", org.springframework.boot.SpringBootVersion.getVersion());
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .header("X-Custom-Header", "Manual-Setup")
                .body(status);
    }

    /**
     * Endpoint demonstrating different HTTP status codes
     * 
     * Test with: curl -i http://localhost:8080/api/test/success
     * Test with: curl -i http://localhost:8080/api/test/error
     */
    @GetMapping("/test/{type}")
    public ResponseEntity<String> testStatus(@PathVariable String type) {
        return switch (type) {
            case "success" -> ResponseEntity.ok("Success response");
            case "created" -> ResponseEntity.status(HttpStatus.CREATED).body("Resource created");
            case "notfound" -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resource not found");
            case "error" -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error");
            default -> ResponseEntity.badRequest().body("Invalid type");
        };
    }

    /**
     * Exception handling example
     * 
     * @ExceptionHandler catches specific exceptions in this controller
     * 
     * Test with: curl http://localhost:8080/api/error
     */
    @GetMapping("/error")
    public String triggerError() {
        throw new RuntimeException("Intentional error for testing");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "RuntimeException");
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}

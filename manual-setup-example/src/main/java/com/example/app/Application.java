package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application Class
 * 
 * @SpringBootApplication is a convenience annotation that combines:
 * 
 * 1. @Configuration
 *    - Marks this class as a source of bean definitions
 *    - Allows defining @Bean methods
 * 
 * 2. @EnableAutoConfiguration
 *    - Enables Spring Boot's auto-configuration mechanism
 *    - Attempts to automatically configure your application based on dependencies
 *    - Example: If spring-boot-starter-web is present, configures embedded Tomcat
 * 
 * 3. @ComponentScan
 *    - Scans for @Component, @Service, @Repository, @Controller in current package
 *    - Registers them as Spring beans
 *    - Default: scans package of this class and all sub-packages
 * 
 * What happens when you run this:
 * 1. SpringApplication.run() starts the Spring context
 * 2. Auto-configuration kicks in based on classpath
 * 3. Component scanning finds all annotated classes
 * 4. Beans are created and wired together
 * 5. Embedded Tomcat server starts (from spring-boot-starter-web)
 * 6. Application is ready to handle requests
 */
@SpringBootApplication
public class Application {

    /**
     * Main method - Entry point of the application
     * 
     * @param args Command line arguments
     * 
     * SpringApplication.run() does:
     * 1. Creates ApplicationContext
     * 2. Registers shutdown hooks
     * 3. Starts embedded server
     * 4. Triggers auto-configuration
     * 5. Publishes application events
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    /**
     * Alternative: Customized SpringApplication
     * 
     * If you need more control over the startup process:
     */
    /*
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        
        // Customize application
        app.setBannerMode(Banner.Mode.OFF);  // Disable banner
        app.setAdditionalProfiles("dev");     // Set active profile
        app.setLogStartupInfo(true);          // Log startup info
        
        app.run(args);
    }
    */
}

package com.smartemergency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Smart Emergency Medical Response & Hospital Bed Management System
 * Main Application Entry Point
 *
 * @author Smart Emergency Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SmartEmergencyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEmergencyApplication.class, args);
        System.out.println("""
                
                ╔══════════════════════════════════════════════════════════╗
                ║   Smart Emergency Medical Response System Started!       ║
                ║   Backend API running at: http://localhost:8080/api      ║
                ║   Version: 1.0.0 | Environment: Development              ║
                ╚══════════════════════════════════════════════════════════╝
                """);
    }
}

package com.river.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for River Platform
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.river"})
public class RiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiverApplication.class, args);
    }
}

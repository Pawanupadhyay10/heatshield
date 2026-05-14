package com.heatshield.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RoutingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoutingApplication.class, args);
    }
}

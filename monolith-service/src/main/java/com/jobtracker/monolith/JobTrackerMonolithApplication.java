package com.jobtracker.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobTrackerMonolithApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobTrackerMonolithApplication.class, args);
    }
}

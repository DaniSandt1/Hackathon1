package com.oreo.insight_factory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OreoHk1Application {
    public static void main(String[] args) {
        SpringApplication.run(OreoHk1Application.class, args);
    }
}

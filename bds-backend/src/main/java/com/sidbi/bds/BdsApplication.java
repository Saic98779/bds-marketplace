package com.sidbi.bds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.sidbi.bds")
public class BdsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BdsApplication.class, args);
    }
}

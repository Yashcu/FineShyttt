package com.yash.fineshyttt;

import com.yash.fineshyttt.config.MediaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MediaProperties.class)
public class FineshytttApplication {

    public static void main(String[] args) {
        SpringApplication.run(FineshytttApplication.class, args);
    }

}
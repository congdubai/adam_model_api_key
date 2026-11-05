package com.adamtts.studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AdamTtsStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdamTtsStudioApplication.class, args);
    }
}


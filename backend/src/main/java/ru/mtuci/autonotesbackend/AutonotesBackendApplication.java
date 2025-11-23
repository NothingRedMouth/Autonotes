package ru.mtuci.autonotesbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AutonotesBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutonotesBackendApplication.class, args);
    }
}

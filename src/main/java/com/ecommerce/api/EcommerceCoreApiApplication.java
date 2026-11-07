package com.ecommerce.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EcommerceCoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceCoreApiApplication.class, args);
    }

}
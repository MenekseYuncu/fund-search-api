package com.menekseyuncu.fundsearchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FundSearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundSearchServiceApplication.class, args);
    }

}

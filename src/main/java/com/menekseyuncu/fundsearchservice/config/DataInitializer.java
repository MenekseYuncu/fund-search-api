package com.menekseyuncu.fundsearchservice.config;

import com.menekseyuncu.fundsearchservice.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * The DataInitializer class is a component that implements the CommandLineRunner interface.
 * It is used to perform data initialization tasks at the application startup.
 * <p>
 * This class relies on the FundService to handle the actual data initialization logic.
 * The run method is automatically executed when the application starts, triggering the initialization process.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FundService fundService;

    @Override
    public void run(String... args) {
        fundService.initializeData();
    }
}

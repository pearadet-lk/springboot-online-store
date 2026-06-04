package com.onlinestore.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SimulatorProperties.class)
public class CheckoutSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutSimulatorApplication.class, args);
    }
}

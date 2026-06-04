package com.onlinestore.tests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiVersioningIntegrationTest.TestApp.class)
@AutoConfigureMockMvc
class ApiVersioningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void versionedRoute_returnsVersionHeader() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("api-supported-versions", "v1"));
    }

    @Test
    void unversionedRoute_stillWorks() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("api-supported-versions", "v1"));
    }

    @SpringBootApplication(scanBasePackages = "com.onlinestore.shared")
    static class TestApp {

        @RestController
        static class HealthController {
            @GetMapping("/health")
            Map<String, String> health() {
                return Map.of("status", "ok");
            }
        }
    }
}

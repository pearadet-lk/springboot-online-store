package com.onlinestore.tests;

import com.onlinestore.gateway.GatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = GatewayApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void health_isPublic() {
        WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()
                .get()
                .uri("/health")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void products_withoutJwt_returnsUnauthorized() {
        WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()
                .get()
                .uri("/api/products")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}

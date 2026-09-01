package com.rrhh.gateway;

import com.rrhh.gateway.config.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class GatewaySecurityIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void rechazaSinToken() {
        webTestClient.get().uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void permiteHealthSinToken() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}

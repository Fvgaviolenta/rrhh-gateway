package com.rrhh.gateway;

import com.rrhh.gateway.config.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
@TestPropertySource(properties = {
        "rrhh.rate-limit.backend=memory",
        "rrhh.rate-limit.replenish=1",
        "rrhh.rate-limit.burst=3"
})
class GatewayRateLimitIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void superaBurstDevuelve429() {
        for (int i = 0; i < 3; i++) {
            webTestClient.get().uri("/api/v1/auth/me")
                    .exchange()
                    .expectStatus()
                    .value(status -> org.junit.jupiter.api.Assertions.assertNotEquals(
                            HttpStatus.TOO_MANY_REQUESTS.value(), status));
        }
        webTestClient.get().uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(429);
    }
}

package com.rrhh.gateway;

import com.rrhh.gateway.config.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
class GatewayApiManagerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void catalogoEsPublico() {
        webTestClient.get().uri("/api/v1/catalogo")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo(200)
                .jsonPath("$.datos.api_manager").isEqualTo("rrhh-gateway")
                .jsonPath("$.datos.apis").isArray();
    }

    @Test
    void catalogoNoConsumeCuota() {
        for (int i = 0; i < 8; i++) {
            webTestClient.get().uri("/api/v1/catalogo")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Test
    void healthSiguePublico() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}

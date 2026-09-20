package com.rrhh.gateway.controller;

import com.rrhh.gateway.config.RateLimitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ApiCatalogController {

    private final RateLimitProperties rateLimit;

    public ApiCatalogController(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    @GetMapping("/api/v1/catalogo")
    public Map<String, Object> catalogo() {
        return Map.of(
                "codigo", 200,
                "mensaje", "Catálogo de APIs gestionadas por rrhh-gateway",
                "datos", Map.of(
                        "api_manager", "rrhh-gateway",
                        "version", "v1",
                        "politicas", Map.of(
                                "auth", "JWT emitido por AWS Cognito",
                                "rate_limit_por_segundo", rateLimit.getReplenish(),
                                "rate_limit_burst", rateLimit.getBurst(),
                                "max_body", "1MB",
                                "connect_timeout_ms", 3000,
                                "response_timeout", "15s"
                        ),
                        "apis", List.of(
                                api("identity", "/api/v1/auth/**, /api/v1/usuarios/**, /api/v1/tenants/**, /api/v1/plataforma/**",
                                        "JWT; resolver de empresa es público; plataforma solo OperadorSaaS"),
                                api("trabajadores", "/api/v1/trabajadores/**, /api/v1/departamentos/**, /api/v1/cargos/**",
                                        "JWT; mutaciones Admin de RRHH / SuperAdmin"),
                                api("contratos", "/api/v1/contratos/**, /api/v1/liquidaciones/**",
                                        "JWT; POST/PATCH Admin de RRHH / SuperAdmin"),
                                api("asistencia", "/api/v1/asistencia/**, /api/v1/marcas-asistencia/**",
                                        "JWT; OperadorSaaS no opera marcas"),
                                api("ausencias", "/api/v1/ausencias/**, /api/v1/solicitudes-ausencia/**",
                                        "JWT; OperadorSaaS no opera ausencias"),
                                api("notifications", "/api/v1/notificaciones/**", "JWT")
                        )
                ),
                "errores", List.of()
        );
    }

    private static Map<String, String> api(String dominio, String rutas, String acceso) {
        return Map.of("dominio", dominio, "rutas", rutas, "acceso", acceso);
    }
}

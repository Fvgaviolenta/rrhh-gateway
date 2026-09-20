package com.rrhh.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rrhh.rate-limit")
public class RateLimitProperties {

    /**
     * Tokens añadidos por segundo (cubo en memoria). En Redis se usa como tope de la ventana.
     */
    private int replenish = 30;

    /**
     * Máximo de peticiones por segundo por cliente (JWT sub o IP).
     */
    private int burst = 60;

    /**
     * auto: Redis si hay host; memory en tests o si Redis no está.
     */
    private String backend = "auto";

    public int getReplenish() {
        return replenish;
    }

    public void setReplenish(int replenish) {
        this.replenish = replenish;
    }

    public int getBurst() {
        return burst;
    }

    public void setBurst(int burst) {
        this.burst = burst;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }
}

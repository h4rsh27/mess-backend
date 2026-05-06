package com.smartmess.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private final Qr qr = new Qr();
    private final Meal meal = new Meal();
    private final Wallet wallet = new Wallet();

    @Data
    public static class Qr {
        private String secret;
        private int refreshMinutes;
    }

    @Data
    public static class Meal {
        private int cutoffMinutes;
        private int graceMinutes;
    }

    @Data
    public static class Wallet {
        private int lowBalanceThreshold;
    }
}

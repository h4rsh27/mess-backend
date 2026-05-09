package com.smartmess.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Typed configuration properties for all custom {@code app.*} keys.
 * <p>
 * Backed by {@code @ConfigurationProperties} so the Spring Boot annotation
 * processor generates IDE metadata — eliminating "unknown property" warnings
 * for every {@code app.*} key in {@code application.properties}.
 * <p>
 * To refresh IDE hints: run {@code mvn compile} once so the processor writes
 * {@code META-INF/spring-configuration-metadata.json} into {@code target/}.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private final Qr     qr     = new Qr();
    private final Meal   meal   = new Meal();
    private final Wallet wallet = new Wallet();
    private final Cors   cors   = new Cors();
    private final Mail   mail   = new Mail();

    /** Maps to {@code app.qr.*} */
    @Data
    public static class Qr {
        /** {@code app.qr.secret} – HMAC key used to sign QR tokens. */
        private String secret;
        /** {@code app.qr.refresh-minutes} – How often rotating QR changes. */
        private int refreshMinutes;
    }

    /** Maps to {@code app.meal.*} */
    @Data
    public static class Meal {
        /** {@code app.meal.cutoff-minutes} – Minutes before meal when booking closes. */
        private int cutoffMinutes;
        /** {@code app.meal.grace-minutes} – Minutes after meal when attendance still accepted. */
        private int graceMinutes;
    }

    /** Maps to {@code app.wallet.*} */
    @Data
    public static class Wallet {
        /** {@code app.wallet.low-balance-threshold} – Wallet balance (₹) below which an alert fires. */
        private int lowBalanceThreshold;
    }

    /** Maps to {@code app.cors.*} */
    @Data
    public static class Cors {
        /**
         * {@code app.cors.allowed-origins} – Comma-separated list of allowed origins.
         * Use {@code *} (default) to allow all; in production set to your exact
         * Vercel URL, e.g. {@code https://mess-frontend-mocha.vercel.app}.
         */
        private String allowedOrigins = "*";
    }

    /** Maps to {@code app.mail.*} */
    @Data
    public static class Mail {
        /** {@code app.mail.enabled} – Set to {@code true} to enable e-mail notifications. */
        private boolean enabled;
        /** {@code app.mail.from} – The From address used in outgoing e-mails. */
        private String from = "noreply@smartmess.com";
    }
}


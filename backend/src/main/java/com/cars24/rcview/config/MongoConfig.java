package com.cars24.rcview.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Custom MongoDB client configuration that:
 * 1. Sets a trust-all SSL context as JVM default (corporate proxy / cert workaround)
 * 2. Creates the MongoClient manually with error handling, so the app still starts
 *    even if DNS SRV lookup or SSL handshake fails.
 */
@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);
    private static final int SERVER_SELECTION_TIMEOUT_SECONDS = 5;
    private static final String FALLBACK_URI = "mongodb://localhost:27017/rcview";

    private static final SSLContext TRUST_ALL_TLS12;

    // Set trust-all SSL as JVM default EARLY — before any MongoDB / HTTPS connections.
    // Forces TLSv1.2 to avoid handshake failures with MongoDB Atlas on Java 17+/25.
    static {
        SSLContext ctx = null;
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}
                    }
            };
            // Use explicit TLSv1.2 — MongoDB Atlas doesn't always negotiate TLSv1.3 with Java 25
            ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            SSLContext.setDefault(ctx);
        } catch (Exception e) {
            System.err.println("[MongoConfig] Failed to set trust-all SSL default: " + e.getMessage());
        }
        TRUST_ALL_TLS12 = ctx;
    }

    @Value("${spring.data.mongodb.uri:" + FALLBACK_URI + "}")
    private String mongoUri;

    /**
     * Manually create MongoClient so that DNS SRV lookup failures (common on
     * corporate networks) don't crash the app at startup.
     */
    @Bean
    public MongoClient mongoClient() {
        MongoClientSettings settings = buildSettings(mongoUri);
        if (settings != null) {
            try {
                MongoClient client = MongoClients.create(settings);
                log.info("MongoClient created with configured URI");
                return client;
            } catch (Exception e) {
                log.warn("MongoClient creation failed: {}", e.getMessage());
            }
        }

        // Fallback: localhost client (will fail at runtime → services use in-memory store)
        log.warn("Using fallback localhost MongoDB URI — DB operations will fail, in-memory fallback active");
        return MongoClients.create(buildSettings(FALLBACK_URI));
    }

    private MongoClientSettings buildSettings(String uri) {
        try {
            ConnectionString cs = new ConnectionString(uri);
            MongoClientSettings.Builder builder = MongoClientSettings.builder()
                    .applyConnectionString(cs)
                    .applyToClusterSettings(b ->
                            b.serverSelectionTimeout(SERVER_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            // Only enable SSL for remote URIs (SRV or explicit ssl=true).
            // Localhost connections don't need SSL.
            boolean needsSsl = uri.startsWith("mongodb+srv://")
                    || uri.contains("ssl=true") || uri.contains("tls=true");
            if (needsSsl) {
                if (TRUST_ALL_TLS12 != null) {
                    builder.applyToSslSettings(ssl -> ssl
                            .enabled(true)
                            .invalidHostNameAllowed(true)
                            .context(TRUST_ALL_TLS12));
                } else {
                    builder.applyToSslSettings(ssl -> ssl
                            .enabled(true)
                            .invalidHostNameAllowed(true));
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse MongoDB URI '{}': {}", maskUri(uri), e.getMessage());
            return null;
        }
    }

    /** Mask credentials in URI for safe logging */
    private String maskUri(String uri) {
        return uri.replaceAll("://[^@]+@", "://***:***@");
    }
}

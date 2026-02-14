package com.cars24.rcview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = { MongoAutoConfiguration.class })
public class RcViewApplication {

    public static void main(String[] args) {
        // Use TLS 1.2+ for MongoDB Atlas; avoids "Received fatal alert: internal_error" on some JDKs/corporate networks
        if (System.getProperty("javax.net.ssl.protocols") == null) {
            System.setProperty("javax.net.ssl.protocols", "TLSv1.2,TLSv1.3");
        }
        if (System.getProperty("jdk.tls.client.protocols") == null) {
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3");
        }
        SpringApplication.run(RcViewApplication.class, args);
    }
}

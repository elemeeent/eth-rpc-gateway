package com.demo;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> LOGGER.info("MainVerticle deployed with id: {}", id))
                .onFailure(err -> {
                    LOGGER.error("Failed to deploy MainVerticle. Reason: {}", err.getMessage(), err);
                    vertx.close();
                    System.exit(1);
                });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down...");
            vertx.close();
        }));
    }
}

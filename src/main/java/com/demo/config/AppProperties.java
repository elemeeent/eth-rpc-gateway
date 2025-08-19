package com.demo.config;

import io.vertx.core.json.JsonObject;

public record AppProperties(
        String ethUrl, int port, boolean keepAlive, int pool, int reqMs, int connMs,
        String certPath, String keyPath
) {
    public static AppProperties from(JsonObject cfg) {
        var http = cfg.getJsonObject("http", new JsonObject());
        var tls  = cfg.getJsonObject("tls", new JsonObject());
        var eth  = cfg.getJsonObject("eth", new JsonObject());

        var timeouts = http.getJsonObject("timeouts", new JsonObject());
        return new AppProperties(
                eth.getString("url"),
                http.getInteger("port", 8443),
                http.getBoolean("keepAlive", true),
                http.getInteger("pool", 200),
                timeouts.getInteger("requestMs", 15_000),
                timeouts.getInteger("connectMs", 5_000),
                tls.getString("certPath"),
                tls.getString("keyPath")
        );
    }
}

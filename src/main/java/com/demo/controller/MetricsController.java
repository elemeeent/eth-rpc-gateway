package com.demo.controller;

import com.demo.model.Headers;
import com.demo.tracker.MethodCallTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.ext.web.Router;

public class MetricsController {
    private final ObjectMapper mapper;

    public MetricsController(ObjectMapper objectMapper, Router router, MethodCallTracker tracker) {
        this.mapper = objectMapper;

        router.get("/metrics")
                .handler(ctx -> {
                    try {
                        ctx.response()
                                .putHeader(Headers.Names.CONTENT_TYPE, Headers.Values.APPLICATION_JSON)
                                .end(mapper.writeValueAsString(tracker.snapshot()));
                    } catch (Exception e) {
                        ctx.response()
                                .setStatusCode(500)
                                .end("{\"error\":\"metrics failed\"}");
                    }
                });
    }
}

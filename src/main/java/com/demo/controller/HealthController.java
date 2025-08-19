package com.demo.controller;

import io.vertx.ext.web.Router;

public class HealthController {

    public HealthController(Router router) {
        router.get("/health").handler(ctx -> ctx.response().end("Ok"));
    }
}

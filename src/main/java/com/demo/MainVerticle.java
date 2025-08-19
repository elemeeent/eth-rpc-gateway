package com.demo;

import com.demo.client.EthereumClient;
import com.demo.config.AppConfig;
import com.demo.config.AppProperties;
import com.demo.controller.HealthController;
import com.demo.tracker.MethodCallTracker;
import com.demo.controller.MetricsController;
import com.demo.controller.RpcController;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.PlatformHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Router router;

    @Override
    public void start(Promise<Void> startPromise) {
        AppConfig appConfig = new AppConfig("local", vertx);
        appConfig.load()
                .onSuccess(properties -> {
                    this.router = Router.router(Vertx.vertx(new VertxOptions().setWorkerPoolSize(properties.pool())));
                    setupHandlers(List.of(
                            ResponseTimeHandler.create(),
                            LoggerHandler.create(),
                            TimeoutHandler.create(properties.reqMs())
                    ));

                    // controllers
                    new HealthController(router);

                    MethodCallTracker methodCallTracker = new MethodCallTracker();
                    new MetricsController(objectMapper, router, methodCallTracker);

                    EthereumClient ethClient = new EthereumClient(vertx, properties);
                    new RpcController(objectMapper, router, ethClient, methodCallTracker);

                    // http server
                    HttpServerOptions httpServerOptions = getHttpServerOptions(properties);
                    vertx.createHttpServer(httpServerOptions)
                            .requestHandler(router)
                            .listen(properties.port(), "0.0.0.0")
                            .onSuccess(success -> {
                                LOGGER.info("Server started on port: {} with ssl: {}", success.actualPort(), httpServerOptions.isSsl());
                                startPromise.complete();
                            })
                            .onFailure(err -> {
                                LOGGER.error("Server start failed. Reason: {}", err.getMessage(), err);
                                startPromise.fail(err);
                            });
                }).onFailure(err -> {
                    LOGGER.error(
                            "Could not start MainVerticle because of configuration load failure. Reason: {}",
                            err.getMessage(), err
                    );
                    startPromise.fail(err);
                });
    }

    private void setupHandlers(List<PlatformHandler> handlerList) {
        for (PlatformHandler platformHandler : handlerList) {
            router.route().handler(platformHandler);
        }
    }

    private HttpServerOptions getHttpServerOptions(AppProperties properties) {
        return new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(
                        new PemKeyCertOptions()
                                .setCertPath(properties.certPath())
                                .setKeyPath(properties.keyPath())
                )
                .setCompressionSupported(true)
                .setTcpKeepAlive(properties.keepAlive())
                .setUseAlpn(false)
                .setIdleTimeout(properties.reqMs() / 1000)
                .setHandle100ContinueAutomatically(true)
                .setEnabledSecureTransportProtocols(Set.of("TLSv1.3", "TLSv1.2"))
                .setPort(properties.port());
    }

}

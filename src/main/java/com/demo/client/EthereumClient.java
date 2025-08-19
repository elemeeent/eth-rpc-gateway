package com.demo.client;

import com.demo.config.AppProperties;
import com.demo.model.Headers;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;

public class EthereumClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EthereumClient.class);

    private final HttpClient client;
    private final String host;
    private final int port;
    private final String scheme;
    private final String basePath;
    private final AppProperties props;

    public EthereumClient(Vertx vertx, AppProperties props) {
        this.props = props;

        URI uri = URI.create(props.ethUrl());
        this.scheme = Optional.ofNullable(uri.getScheme()).orElse("http");
        boolean ssl = "https".equalsIgnoreCase(this.scheme);

        this.host = uri.getHost();
        this.port = (uri.getPort() == -1) ? (ssl ? 443 : 80) : uri.getPort();
        this.basePath = Optional.ofNullable(uri.getRawPath())
                .filter(path -> !path.isBlank())
                .orElse("/");

        HttpClientOptions opts = new HttpClientOptions()
                .setSsl(ssl)
                .setDefaultHost(host)
                .setDefaultPort(port)
                .setKeepAlive(props.keepAlive())
                .setIdleTimeout(props.reqMs())
                .setConnectTimeout(props.connMs());

        this.client = vertx.createHttpClient(opts);

        LOGGER.info("EthereumClient -> {}://{}:{}{}", scheme, host, port, basePath);
    }

    public Future<Void> forward(Buffer rawBody, HttpServerResponse clientResp) {
        RequestOptions opts = new RequestOptions()
                .setMethod(HttpMethod.POST)
                .setHost(host)
                .setPort(port)
                .setURI(basePath)
                .setTimeout(props.reqMs());

        return client.request(opts)
                .compose(req -> {
                    req.putHeader(Headers.Names.CONTENT_TYPE, Headers.Values.APPLICATION_JSON);
                    return req.send(rawBody);
                })
                .compose(upstream -> {
                    clientResp.setStatusCode(upstream.statusCode());
                    upstream.headers().forEach(h -> {
                        if (!"transfer-encoding".equalsIgnoreCase(h.getKey())) {
                            clientResp.putHeader(h.getKey(), h.getValue());
                        }
                    });
                    return upstream.pipeTo(clientResp);
                });
    }
}

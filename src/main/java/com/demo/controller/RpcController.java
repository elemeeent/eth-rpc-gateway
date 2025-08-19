package com.demo.controller;

import com.demo.client.EthereumClient;
import com.demo.model.Headers;
import com.demo.tracker.MethodCallTracker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class RpcController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcController.class);
    private static final String UPSTREAM_ERROR_MESSAGE = "Upstream error";
    private final ObjectMapper mapper;

    public RpcController(ObjectMapper objectMapper, Router router, EthereumClient eth, MethodCallTracker tracker) {
        this.mapper = objectMapper;

        router.post("/")
                .handler(BodyHandler.create(false))
                .handler(ctx -> {
                    Buffer body = ctx.body().buffer();
                    JsonNode parsed = parseNodeAndUpdateMetrics(tracker, body);

                    eth.forward(body, ctx.response())
                            .onFailure(err -> {
                                LOGGER.error("{}: {}", UPSTREAM_ERROR_MESSAGE, err.getMessage(), err);
                                byte[] bytes = buildErrorForRequest(parsed, -32000, UPSTREAM_ERROR_MESSAGE);
                                ctx.response().setStatusCode(502)
                                        .putHeader(Headers.Names.CONTENT_TYPE, Headers.Values.APPLICATION_JSON)
                                        .end(Buffer.buffer(bytes));
                            });
                });
    }

    private JsonNode parseNodeAndUpdateMetrics(MethodCallTracker tracker, Buffer body) {
        JsonNode parsed = null;

        try {
            parsed = mapper.readTree(body.getBytes());
            if (parsed.isArray()) {
                for (JsonNode el : parsed) {
                    JsonNode m = el.get("method");
                    if (m != null && m.isTextual()) tracker.inc(m.asText());
                }
            } else {
                JsonNode m = parsed.get("method");
                if (m != null && m.isTextual()) tracker.inc(m.asText());
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse JSON-RPC for counting: {}", ex.toString());
        }
        return parsed;
    }

    private byte[] buildErrorForRequest(JsonNode reqRoot, int code, String message) {
        try {
            if (reqRoot != null && reqRoot.isArray()) {
                ArrayNode arr = mapper.createArrayNode();
                for (JsonNode nodeElement : reqRoot) {
                    arr.add(mapper.writeValueAsBytes(generateErrorNode(nodeElement, code, message)));
                }
                return mapper.writeValueAsBytes(arr);
            } else {
                return mapper.writeValueAsBytes(generateErrorNode(reqRoot, code, message));
            }
        } catch (Exception e) {
            return generateErrorNode(
                    null,
                    -32000,
                    UPSTREAM_ERROR_MESSAGE)
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private JsonNode generateErrorNode(JsonNode node, int code, String message) {
        String jsonrpcParam = "jsonrpc";
        String errorParam = "error";
        String codeParam = "code";
        String messageParam = "message";

        if (node == null) {
            return mapper.createObjectNode()
                    .put(jsonrpcParam, "2.0")
                    .set(errorParam, mapper.createObjectNode()
                            .put(codeParam, code)
                            .put(messageParam, message));
        }

        String id = node.get("id") != null && !node.get("id").asText().isBlank()
                ? node.get("id").asText()
                : "unknown";

        return mapper.createObjectNode()
                .put("id", id)
                .put(jsonrpcParam, "2.0")
                .set(errorParam, mapper.createObjectNode()
                        .put(codeParam, code)
                        .put(messageParam, message));
    }
}

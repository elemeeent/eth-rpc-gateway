package com.demo.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    private final String envName;
    private final Vertx vertx;

    public AppConfig(String envName, Vertx vertx) {
        this.envName = envName;
        this.vertx = vertx;
    }

    public Future<AppProperties> load() {
        String configForEnv = String.format("config-%s.yml", this.envName);
        try (var configFile = getClass().getClassLoader().getResourceAsStream(configForEnv)) {
            if (configFile == null) {
                LOGGER.error("Could not read file {}. File not found or no permissions.", configForEnv);
                return Future.failedFuture("Cloud not start application due the config file reading error");
            }
            var yamlFile = new ConfigStoreOptions()
                    .setType("json")
                    .setConfig(configToJson(configFile));

            var env = new ConfigStoreOptions().setType("env");
            var sys = new ConfigStoreOptions().setType("sys");

            var options = new ConfigRetrieverOptions().addStore(yamlFile).addStore(env).addStore(sys);
            return ConfigRetriever.create(this.vertx, options)
                    .getConfig()
                    .map(AppProperties::from);
        } catch (IOException e) {
            return Future.failedFuture(e);
        }
    }

    private JsonObject configToJson(InputStream configFile) throws IOException {
        String yaml = new String(configFile.readAllBytes(), StandardCharsets.UTF_8);
        Yaml parser = new Yaml();
        Object data = parser.load(yaml);
        return JsonObject.mapFrom(data);
    }

}

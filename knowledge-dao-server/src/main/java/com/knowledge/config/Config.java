package com.knowledge.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    private static Config instance;
    private final DbConfig db;
    private final OllamaConfig ollama;
    private final AppConfig app;

    public static class DbConfig {
        public String host = "localhost";
        public int port = 5432;
        public String database = "knowledge";
        public String username = "dou";
        public String password;

        public String getJdbcUrl() {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        }

        /**
         * Get password from environment variable DB_PASSWORD, or falls back to config file value.
         */
        public String getPassword() {
            String envPassword = System.getenv("DB_PASSWORD");
            return (envPassword != null && !envPassword.isBlank()) ? envPassword : password;
        }
    }

    public static class OllamaConfig {
        @JsonProperty("base-url")
        public String baseUrl = "http://localhost:11434";
        @JsonProperty("embedding-model")
        public String embeddingModel = "bge-m3";
    }

    public static class AppConfig {
        @JsonProperty("embedding-dimension")
        public int embeddingDimension = 1024;
    }

    // Jackson 需要默认构造函数
    public Config() {
        this.db = new DbConfig();
        this.ollama = new OllamaConfig();
        this.app = new AppConfig();
    }

    private Config(DbConfig db, OllamaConfig ollama, AppConfig app) {
        this.db = db;
        this.ollama = ollama;
        this.app = app;
    }

    public static synchronized Config load() throws IOException {
        if (instance != null) return instance;

        // Try loading from current directory first, then classpath
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Config loaded;

        Path localPath = Paths.get("config.yaml");
        if (Files.exists(localPath)) {
            loaded = mapper.readValue(localPath.toFile(), Config.class);
        } else {
            try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config.yaml")) {
                if (is == null) {
                    throw new IOException("config.yaml not found in classpath or current directory");
                }
                loaded = mapper.readValue(is, Config.class);
            }
        }

        instance = loaded;
        return instance;
    }

    public DbConfig getDb() { return db; }
    public OllamaConfig getOllama() { return ollama; }
    public AppConfig getApp() { return app; }
}

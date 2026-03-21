package com.knowledge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI knowledgeDaoOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Knowledge DAO API")
                .description("本地 RAG 知识库 API 文档")
                .version("2.0.0")
                .contact(new Contact().name("rag-dev")));
    }
}

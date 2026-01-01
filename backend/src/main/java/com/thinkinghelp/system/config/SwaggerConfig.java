package com.thinkinghelp.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个性化慢病膳食系统 API")
                        .version("1.0")
                        .description("ThinkingHelp 系统 API 文档")
                        .contact(new Contact().name("ThinkingHelp").email("support@thinkinghelp.com")));
    }
}

package com.declaration.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j OpenAPI 3 配置类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("线索申报系统API文档")
                .version("1.0")
                .description("基于SpringBoot + MyBatis-Plus + Sa-Token构建的线索申报系统")
                .contact(new Contact()
                    .name("开发团队")
                    .email("")))
            .components(new Components()
                // 头名必须与 sa-token.token-name 一致，否则 Swagger 里填了 token 也过不了登录拦截器
                .addSecuritySchemes("lead-decl-token",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .name("lead-decl-token")
                        .in(SecurityScheme.In.HEADER)))
            .addSecurityItem(new SecurityRequirement().addList("lead-decl-token"));
    }
}
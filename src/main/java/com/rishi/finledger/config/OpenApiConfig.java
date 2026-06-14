package com.rishi.finledger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finLedgerOpenAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("FinLedger API")
                .version("1.0")
                .description("Digital Wallet & FinTech Backend")
                .contact(new Contact().name("Rishi").email("rishiugandhar18@gmail.com")))
            .addSecurityItem(
                new SecurityRequirement().addList(securitySchemeName))
            .components(
                new Components()
                    .addSecuritySchemes(securitySchemeName,new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
package co.com.mmartinez.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contractsPricingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Contracts & Pricing API")
                .description("Gestión de contratos de venta y condiciones de precio para nueve países de Latinoamérica.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Miguel Ángel Martínez Flórez")
                        .url("https://github.com/migueflorez10")));
    }
}

package com.supermarket.pos.salesapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI/Swagger documentation.
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Supermarket POS Sales API")
                .version("1.0.0")
                .description("""
                    REST API for managing sales transactions at supermarket POS terminals.
                    
                    ## Features
                    
                    - **Sale Management**: Create, modify, and complete sales
                    - **Item Management**: Add, update, and remove items from sales
                    - **Payment Processing**: Cash and credit payment support
                    - **Freeze/Resume**: Temporarily pause sales and resume them later
                    - **Returns**: Process full and partial returns
                    - **Receipt Generation**: Automatic receipt generation for completed transactions
                    
                    ## External Dependencies
                    
                    This API integrates with:
                    - **Product API**: For product catalog and stock management
                    - **Customer API**: For customer information and credit status
                    """)
                .contact(new Contact()
                    .name("Supermarket POS Team")
                    .email("support@supermarketpos.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}

package com.fpt.payments;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableCaching
@EnableKafka
@OpenAPIDefinition(info = @Info(
        title = "Payment Transaction Service API",
        version = "1.0",
        description = "Banking fund transfer and account management API"
))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class PaymentTransactionApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentTransactionApplication.class, args);
    }
}

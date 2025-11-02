package com.pm.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    // Convention: <FilterName>GatewayFilterFactory
    // JwtValidation => Filter name that can be used in application.yml
    // GatewayFilterFactory => Abstract class provided by Spring cloud gateway

    private final WebClient webClient;

    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
                                             @Value("${auth.service.url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }

    @Override
    public GatewayFilter apply(Object config){
        // Both variables are passed by AbstractGatewayFilterFactory
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if(authHeader == null || !authHeader.startsWith("Bearer ")){
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            return webClient.get()
                    .uri("/validate")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .toBodilessEntity()
                    .then(chain.filter(exchange));
        };
    }

    /*
        By extending AbstractGatewayFilterFactory and over-riding the apply method, we are telling
        Spring cloud gateway to add this filter to Spring boot lifecycle
     */
}

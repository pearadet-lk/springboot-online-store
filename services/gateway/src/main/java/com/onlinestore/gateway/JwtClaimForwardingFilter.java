package com.onlinestore.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtClaimForwardingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    if (ctx.getAuthentication() != null
                            && ctx.getAuthentication().getPrincipal() instanceof Jwt jwt) {
                        var mutated = exchange.mutate().request(builder -> {
                            builder.header("X-Authenticated-UserId", jwt.getSubject());
                            if (jwt.hasClaim("email")) {
                                builder.header("X-Authenticated-Email", jwt.getClaimAsString("email"));
                            }
                            if (jwt.hasClaim("name")) {
                                builder.header("X-Authenticated-Name", jwt.getClaimAsString("name"));
                            }
                            builder.header("X-Correlation-ID", exchange.getRequest().getId());
                        });
                        return chain.filter(mutated.build());
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}

package com.pm.apigateway.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

// RedisRateLimiter loads its Lua script from the classpath at runtime;
// GraalVM native images only include resources registered at build time
@Configuration
@ImportRuntimeHints(RateLimiterRuntimeHints.class)
public class RateLimiterRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("META-INF/scripts/*.lua");
    }
}

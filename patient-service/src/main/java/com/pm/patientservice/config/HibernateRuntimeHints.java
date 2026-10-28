package com.pm.patientservice.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.UUID;

// Hibernate's multi-id loader allocates UUID[] reflectively, which GraalVM native images
// reject unless the array type is registered at build time
@Configuration
@ImportRuntimeHints(HibernateRuntimeHints.class)
public class HibernateRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(UUID[].class, MemberCategory.UNSAFE_ALLOCATED);
    }
}

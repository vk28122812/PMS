package com.pm.authservice.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

// JJWT loads its implementation classes (jjwt-impl, jjwt-jackson) by name via reflection,
// which GraalVM native images strip unless they are registered at build time
@Configuration
@ImportRuntimeHints(JjwtRuntimeHints.class)
public class JjwtRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(classLoader);

        try {
            for (Resource resource : resolver.getResources("classpath*:io/jsonwebtoken/**/*.class")) {
                String className = readerFactory.getMetadataReader(resource).getClassMetadata().getClassName();
                hints.reflection().registerType(TypeReference.of(className),
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.DECLARED_FIELDS);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        hints.resources().registerPattern("META-INF/services/io.jsonwebtoken.*");
    }
}

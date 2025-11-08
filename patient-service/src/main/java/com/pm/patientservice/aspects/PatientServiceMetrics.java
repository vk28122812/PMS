package com.pm.patientservice.aspects;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


// Aspect is a class that encapsulates cross-cutting concerns( things that are common to multiple methods)
@Aspect
@Component
public class PatientServiceMetrics {

    private final MeterRegistry meterRegistry;

    public PatientServiceMetrics(MeterRegistry meterRegistry){
        this.meterRegistry = meterRegistry;
    }

    // PointCut => Expressions that specify the criteria for selecting join points where advice should be applied.
    // ProceedingJoinPoint => JoinPoint is a specific point during program execution, typically a method invocation.
    // Advice => tasks performed after intercepting a join point.

    @Around("execution(* com.pm.patientservice.service.PatientService.getAllPatients(..))")
    public Object monitorGetPatients(ProceedingJoinPoint jointPoint) throws Throwable{
        // Whenenver there is a cache increase the counter for the following tag
        meterRegistry.counter("custom.redis.cache.miss", "cache", "patients")
                .increment();
        Object result = jointPoint.proceed();
        return result;
    }
}

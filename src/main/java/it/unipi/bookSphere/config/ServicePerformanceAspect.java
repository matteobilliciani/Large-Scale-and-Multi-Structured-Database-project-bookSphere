package it.unipi.bookSphere.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServicePerformanceAspect {

    private static final long SLOW_SERVICE_THRESHOLD_MS = 500; // 500ms

    // DISABLED: Aspect intercepts too many methods causing overhead
    // @Around("execution(* it.unipi.bookSphere.service..*.*(..))")
    public Object logServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > SLOW_SERVICE_THRESHOLD_MS) {
                log.warn("🐌 SLOW SERVICE: {}.{} took {}ms", 
                        className.substring(className.lastIndexOf('.') + 1), 
                        methodName, duration);
            } else if (duration > 200) {
                log.debug("⏱️ SERVICE: {}.{} took {}ms", 
                        className.substring(className.lastIndexOf('.') + 1), 
                        methodName, duration);
            }
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ SERVICE ERROR: {}.{} failed after {}ms - {}", 
                    className.substring(className.lastIndexOf('.') + 1), 
                    methodName, duration, e.getMessage());
            throw e;
        }
    }
}

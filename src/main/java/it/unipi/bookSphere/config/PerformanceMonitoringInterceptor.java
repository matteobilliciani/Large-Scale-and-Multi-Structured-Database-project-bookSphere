package it.unipi.bookSphere.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class PerformanceMonitoringInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000; // 1 second

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String fullUrl = queryString != null ? uri + "?" + queryString : uri;
            
            int status = response.getStatus();
            
            // Log all requests with timing
            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("⚠️ SLOW REQUEST: {} {} - {}ms - Status: {}", 
                        method, fullUrl, duration, status);
            } else if (duration > 500) {
                log.info("🐌 MEDIUM REQUEST: {} {} - {}ms - Status: {}", 
                        method, fullUrl, duration, status);
            } else {
                log.debug("✅ FAST REQUEST: {} {} - {}ms - Status: {}", 
                        method, fullUrl, duration, status);
            }
            
            // Log error responses
            if (status >= 500) {
                log.error("❌ SERVER ERROR: {} {} - Status: {} - Duration: {}ms", 
                        method, fullUrl, status, duration);
            } else if (status >= 400) {
                log.warn("⚠️ CLIENT ERROR: {} {} - Status: {} - Duration: {}ms", 
                        method, fullUrl, status, duration);
            }
        }
    }
}

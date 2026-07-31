package com.cosio.lm;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * RateLimiter implements HandleInterceptor
 * Defines the logic for limiting user/guest traffic to endpoints
 */
@Component
public class RateLimiter implements HandlerInterceptor {
    
    /** threadsafe guestID/token */
    ConcurrentHashMap<String, Bucket> bucketMap;

    /**
     * Constructor. Auto-injection by Spring
     * @param bucketMap
     */
    public RateLimiter(ConcurrentHashMap<String, Bucket> bucketMap) {
        this.bucketMap = bucketMap;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {

        // identity lookup
        String session = getCookie(req);

        // bucket creation
        if (session == null) return false;

        if (!bucketMap.containsKey(session)) {
            bucketMap.put(session, Bucket.builder()
                .addLimit(limit -> limit.capacity(10).refillGreedy(2, Duration.ofMinutes(1)))
                .build()
            );
        }

        // bucket lookup
        Bucket bucket = bucketMap.get(session);
        if (bucket == null) return false;

        // tryConsume check
        if(!bucket.tryConsume(1)) {
            resp.setStatus(429);
            return false;
        }

        return true;
    }

    /**
     * Add check for mutual exclusivity?
     * @param req
     * @return
     */
    private String getCookie(HttpServletRequest req) {

        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        // if guestID OR session token are found, return
        for (Cookie c : cookies) {
            if (c.getName().equals("guestID") || c.getName().equals("token")) { 
                    return c.getValue(); 
                }
        }

        return null;
    }

}

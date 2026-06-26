package com.pdk.agents.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Service cache AI responses bằng Redis.
 * Tránh gọi lại mô hình AI cho cùng một input (tiết kiệm tài nguyên + tăng tốc).
 *
 * Key = SHA-256(nodeId + systemPrompt + userInput)
 * Value = AI response string
 * TTL = cấu hình trong application.yaml
 */
@Service
@Slf4j
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    private static final String CACHE_PREFIX = "ai:cache:";

    public CacheService(StringRedisTemplate redisTemplate,
                        @Value("${app.cache.ttl-minutes:60}") int ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * Tìm response đã cache cho input này.
     *
     * @return Response đã cache, hoặc null nếu chưa có
     */
    public String getCachedResponse(String nodeId, String systemPrompt, String userInput) {
        String key = buildKey(nodeId, systemPrompt, userInput);
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            log.info("⚡ Cache HIT for node [{}] — skipping AI call", nodeId);
        }

        return cached;
    }

    /**
     * Lưu AI response vào cache.
     */
    public void cacheResponse(String nodeId, String systemPrompt, String userInput, String response) {
        String key = buildKey(nodeId, systemPrompt, userInput);
        redisTemplate.opsForValue().set(key, response, ttl);
        log.debug("📦 Cached response for node [{}], TTL: {} min", nodeId, ttl.toMinutes());
    }

    /**
     * Xóa cache cho một node cụ thể (khi cần force refresh).
     */
    public void invalidate(String nodeId, String systemPrompt, String userInput) {
        String key = buildKey(nodeId, systemPrompt, userInput);
        redisTemplate.delete(key);
    }

    /**
     * Build cache key = SHA-256 hash của (nodeId + systemPrompt + userInput).
     */
    private String buildKey(String nodeId, String systemPrompt, String userInput) {
        try {
            String raw = nodeId + "|" + (systemPrompt != null ? systemPrompt : "") + "|" + userInput;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return CACHE_PREFIX + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // Fallback nếu SHA-256 không khả dụng (rất hiếm)
            return CACHE_PREFIX + (nodeId + userInput).hashCode();
        }
    }
}

package com.yxhpy.crawl_start.service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisBloomFilterService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLOOM_FILTER_KEY = "message:bloom:filter";

    public RedisBloomFilterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void initializeBloomFilter() {
        // 创建布隆过滤器，预计元素数量为 1000000，错误率为 0.01
        redisTemplate.opsForValue().setBit(BLOOM_FILTER_KEY, 1000000, false);
        redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.execute("BF.RESERVE", BLOOM_FILTER_KEY.getBytes(), "0.01".getBytes(), "1000000".getBytes());
            return true;
        });
    }

    public boolean add(String item) {
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) connection ->
                connection.execute("BF.ADD", BLOOM_FILTER_KEY.getBytes(), item.getBytes()) != null
        ));
    }

    public boolean contains(String item) {
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) connection ->
                connection.execute("BF.EXISTS", BLOOM_FILTER_KEY.getBytes(), item.getBytes()) != null
        ));
    }
}

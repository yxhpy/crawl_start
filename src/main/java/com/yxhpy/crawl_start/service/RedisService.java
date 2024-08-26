package com.yxhpy.crawl_start.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.yxhpy.crawl_start.kconst.RConst.REQUEST_TASK_URL;

@Service
public class RedisService {
    @Resource
    RedisTemplate<String, String> redisTemplate;
    @Resource
    RedisBloomFilterService redisBloomFilterService;


//    public boolean addUrlIfNotExists(String url) {
//        if (!redisBloomFilterService.contains(url)) {
//            // 如果不存在就一定不存在
//            redisTemplate.opsForSet().add(REQUEST_TASK_URL, url);
//            redisBloomFilterService.add(url);
//            return true;
//        } else {
//            Long add = redisTemplate.opsForSet().add(REQUEST_TASK_URL, url);
//            if (add != null && add != 0) {
//                redisBloomFilterService.add(url);
//                return true;
//            }
//        }
//        return false;
//    }

    public boolean addUrlIfNotExists(String url) {
        Long add = redisTemplate.opsForSet().add(REQUEST_TASK_URL, url);
        return add != null && add != 0;
    }

    public void removeUrl(String url) {
        redisTemplate.opsForSet().remove(REQUEST_TASK_URL, url);
    }
}

package com.yxhpy.crawl_start.service;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.yxhpy.crawl_start.kconst.RConst.*;

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


    public void addWordInPage(String word, String url) {
        redisTemplate.opsForSet().add(IDF_COUNT_URL, url);
        redisTemplate.opsForSet().add(IDF_URL + ":" + word, url);
    }

    public void addWordInPage(String word, Set<String> url) {
        redisTemplate.opsForSet().add(IDF_COUNT_URL, url.toArray(new String[0]));
        redisTemplate.opsForSet().add(IDF_URL + ":" + word, url.toArray(new String[0]));
    }


    public Long getWordInPageSize(String word) {
        return redisTemplate.opsForSet().size(IDF_URL + ":" + word);
    }

    public Long getPageCount() {
        return redisTemplate.opsForSet().size(IDF_COUNT_URL);
    }

    public void batchAddWordInPage(Map<String, Set<String>> map) {
        CountDownLatch latch = new CountDownLatch(map.entrySet().size());
        Observable.fromIterable(map.entrySet())
                .subscribeOn(Schedulers.io())
                .flatMap(entry -> Observable.create(emitter -> {
                    addWordInPage(entry.getKey(), entry.getValue());
                    emitter.onComplete();
                }).doFinally(latch::countDown)).subscribe();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public Double getWordIDF(String word) {
        return getPageCount() / (double) getWordInPageSize(word);
    }
}

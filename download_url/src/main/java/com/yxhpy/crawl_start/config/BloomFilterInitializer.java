//package com.yxhpy.crawl_start.config;
//
//import com.yxhpy.crawl_start.service.RedisBloomFilterService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class BloomFilterInitializer implements CommandLineRunner {
//    private final RedisBloomFilterService bloomFilterService;
//
//    @Autowired
//    public BloomFilterInitializer(RedisBloomFilterService bloomFilterService) {
//        this.bloomFilterService = bloomFilterService;
//    }
//
//    @Override
//    public void run(String... args) {
//        bloomFilterService.initializeBloomFilter();
//    }
//}

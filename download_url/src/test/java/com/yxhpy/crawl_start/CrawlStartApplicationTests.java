package com.yxhpy.crawl_start;

import com.yxhpy.TextPreprocessor;
import com.yxhpy.WebContentExtractor;
import com.yxhpy.crawl_start.entity.RequestUrlDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import com.yxhpy.crawl_start.service.RedisService;
import com.yxhpy.crawl_start.utils.UrlDownload;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class CrawlStartApplicationTests {
    @Resource
    UrlDownload urlDownload;
    @Resource
    RedisService redisService;


    @Test
    void contextLoads() {
        List<RequestUrlValueDTO> run = urlDownload.run(Arrays.asList(
                RequestUrlDTO.builder().retryTimes(0).url("https://www.baidu.com").build()
        ));
        RequestUrlValueDTO requestUrlValueDTO = run.get(0);
        String html = requestUrlValueDTO.getHtml();
        Map<String, String> map = WebContentExtractor.extractContent(html);
        String mainContent = map.getOrDefault("meta_description", map.get("main_content"));
        List<String> words = TextPreprocessor.preprocess(mainContent);
        Map<String, Integer> bagOfWords = new HashMap<>();
        for (String word : words) {
            bagOfWords.put(word, bagOfWords.getOrDefault(word, 0) + 1);
        }
        List<Object[]> collect = bagOfWords.entrySet().stream().map(i -> {
            String key = i.getKey();
            Integer value = i.getValue();
            return new Object[]{key, redisService.getWordIDF(key) * (value / (double) words.size())};
        }).sorted((o1, o2) -> {
            int i1 = (int) ((double)o1[1] * 100000);
            int i2 = (int) ((double)o2[1] * 100000);
            return i2 - i1;
        }).collect(Collectors.toList());
        List<Object[]> objects1 = collect.subList(0, 5);
        Set<String> keywords = objects1.stream().map(i -> String.valueOf(i[0])).collect(Collectors.toSet());
        Set<String> urls = redisService.inter(keywords);
        log.info("类似urls {}", urls);
        for (Object[] objects : collect) {
            log.info("词：{} 重要程度：{}", objects[0], objects[1]);
        }
    }

}

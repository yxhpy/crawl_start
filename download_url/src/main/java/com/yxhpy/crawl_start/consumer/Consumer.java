package com.yxhpy.crawl_start.consumer;

import com.yxhpy.TextPreprocessor;
import com.yxhpy.WebContentExtractor;
import com.yxhpy.crawl_start.entity.ParseHtmlValueDTO;
import com.yxhpy.crawl_start.entity.RequestUrlDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import com.yxhpy.crawl_start.kconst.KTopics;
import com.yxhpy.crawl_start.producer.Producer;
import com.yxhpy.crawl_start.service.RedisService;
import com.yxhpy.crawl_start.utils.HtmlParse;
import com.yxhpy.crawl_start.utils.UrlDownload;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Consumer {
    @Resource
    UrlDownload urlDownload;
    @Resource
    HtmlParse htmlParse;
    @Resource
    Producer producer;
    @Resource
    RedisService redisService;

    @PostConstruct
    public void start() {
        producer.sendMessageRequestUrl(KTopics.REQUEST_URL, RequestUrlDTO.builder().url("https://www.hao123.com").retryTimes(0).build());
    }

    @KafkaListener(topics = KTopics.REQUEST_URL, containerFactory = "kafkaListenerContainerFactory")
    public void requestUrl(List<ConsumerRecord<String, RequestUrlDTO>> records, Acknowledgment ack) {
        log.info("接受到请求任务{}个", records.size());
        long start = System.currentTimeMillis();
        List<RequestUrlDTO> collect = records.stream().map(ConsumerRecord::value).collect(Collectors.toList());
        List<RequestUrlValueDTO> run = urlDownload.run(collect);
        ack.acknowledge();
        log.info("接受到请求任务{}个,成功{}个,耗时{}ms", records.size(), run.size(), System.currentTimeMillis() - start);
        for (RequestUrlValueDTO requestUrlValueDTO : run) {
            producer.sendMessageParseHtml(KTopics.PARSE_HTML, requestUrlValueDTO);
        }
    }

    @KafkaListener(topics = KTopics.RETRY_PARSE_HTML, containerFactory = "kafkaListenerContainerFactory2")
    public void retryRequestUrl(List<ConsumerRecord<String, RequestUrlDTO>> records, Acknowledgment ack) {
        log.info("接受到重试任务{}个", records.size());
        long start = System.currentTimeMillis();
        List<RequestUrlDTO> collect = records.stream().map(ConsumerRecord::value).collect(Collectors.toList());
        List<RequestUrlValueDTO> run = urlDownload.run(collect);
        ack.acknowledge();
        log.info("接受到重试任务{}个,成功{}个,耗时{}ms", records.size(), run.size(), System.currentTimeMillis() - start);
        for (RequestUrlValueDTO requestUrlValueDTO : run) {
            producer.sendMessageParseHtml(KTopics.PARSE_HTML, requestUrlValueDTO);
        }
    }

    @KafkaListener(topics = KTopics.PARSE_HTML)
    public void parseUrlBody(List<ConsumerRecord<String, RequestUrlValueDTO>> records, Acknowledgment ack) {
        log.info("接受到解析网页{}个", records.size());
        List<RequestUrlValueDTO> collect1 = records.stream().map(ConsumerRecord::value).collect(Collectors.toList());
        List<ParseHtmlValueDTO> run = htmlParse.run(collect1);
        ack.acknowledge();
        for (ParseHtmlValueDTO parseHtmlValueDTO : run) {
            for (String url : parseHtmlValueDTO.getUrls()) {
                producer.sendMessageRequestUrl(KTopics.REQUEST_URL, RequestUrlDTO.builder().url(url).retryTimes(0).build());
            }
        }
    }


    @KafkaListener(topics = KTopics.PARSE_HTML, groupId = "parse_test_02")
    public void parseWords(List<ConsumerRecord<String, RequestUrlValueDTO>> records, Acknowledgment ack) {
        log.info("接受到解析网页分词{}个", records.size());
        long start = System.currentTimeMillis();
        List<RequestUrlValueDTO> values = records.stream().map(ConsumerRecord::value).collect(Collectors.toList());
        Map<String, Set<String>> mapSet = new HashMap<>();
        values.forEach(value -> {
            String html = value.getHtml();
            String url = value.getUrl();
            Map<String, String> map = WebContentExtractor.extractContent(html);
            String mainContent = map.getOrDefault("meta_description", map.get("main_content"));
            List<String> words = TextPreprocessor.preprocess(mainContent);
            // 词频率
            Map<String, Integer> bagOfWords = new HashMap<>();
            for (String word : words) {
                bagOfWords.put(word, bagOfWords.getOrDefault(word, 0) + 1);
            }
            for (String key : bagOfWords.keySet()) {
                Set<String> strings = mapSet.computeIfAbsent(key, k -> new HashSet<>());
                strings.add(url);
            }
        });
        log.info("接受到解析网页分词{}个,解析出{}个词,耗时{}ms", records.size(), mapSet.size(), System.currentTimeMillis() - start);
        redisService.batchAddWordInPage(mapSet);
        log.info("接受到解析网页分词{}个,解析出{}个词,耗时{}ms", records.size(), mapSet.size(), System.currentTimeMillis() - start);
        ack.acknowledge();
    }
}

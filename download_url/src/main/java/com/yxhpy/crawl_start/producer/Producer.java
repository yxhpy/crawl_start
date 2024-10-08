package com.yxhpy.crawl_start.producer;


import com.yxhpy.crawl_start.entity.RequestUrlDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import com.yxhpy.crawl_start.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class Producer {
    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Resource
    RedisService redisService;


    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message, message);
    }

    public void sendMessageRequestUrl(String topic, RequestUrlDTO requestUrlDTO) {
        String url = requestUrlDTO.getUrl();
        if (redisService.addUrlIfNotExists(url)) {
            kafkaTemplate.send(topic, requestUrlDTO.getUrl(), requestUrlDTO)
                    .addCallback((result -> {
                        // 发送成功就将其添加到redis中，防止重复添加
                    }), ex -> {
                        log.error(ex.getMessage());
                        redisService.removeUrl(url);
                    });
        }
    }

    public void sendMessageRetryRequestUrl(String topic, RequestUrlDTO requestUrlDTO) {
        kafkaTemplate.send(topic, requestUrlDTO.getUrl(), requestUrlDTO);
    }

    public void sendMessageParseHtml(String topic, RequestUrlValueDTO requestUrlValueDTO) {
        kafkaTemplate.send(topic, requestUrlValueDTO.getUrl(), requestUrlValueDTO);
    }
}

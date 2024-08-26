package com.yxhpy.crawl_start.consumer;

import com.yxhpy.crawl_start.entity.ParseHtmlDTO;
import com.yxhpy.crawl_start.entity.ParseHtmlValueDTO;
import com.yxhpy.crawl_start.entity.RequestUrlDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import com.yxhpy.crawl_start.kconst.KTopics;
import com.yxhpy.crawl_start.producer.Producer;
import com.yxhpy.crawl_start.utils.HtmlParse;
import com.yxhpy.crawl_start.utils.UrlDownload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Consumer {
    @Resource
    UrlDownload urlDownload;
    @Resource
    HtmlParse htmlParse;
    @Resource
    Producer producer;

    @PostConstruct
    public void start(){
        producer.sendMessageRequestUrl(KTopics.REQUEST_URL, RequestUrlDTO.builder().url("https://www.hao123.com").build());
    }

    @KafkaListener(topics = KTopics.REQUEST_URL, containerFactory = "kafkaListenerContainerFactory")
    public void requestUrl(List<ConsumerRecord<String, RequestUrlDTO>> records, Acknowledgment ack) {
        List<String> collect = records.stream().map(ConsumerRecord::value).map(RequestUrlDTO::getUrl).collect(Collectors.toList());
        List<RequestUrlValueDTO> run = urlDownload.run(collect);
        ack.acknowledge();
        for (RequestUrlValueDTO requestUrlValueDTO : run) {
            producer.sendMessageParseHtml(KTopics.PARSE_HTML, requestUrlValueDTO);
        }

    }

    @KafkaListener(topics = KTopics.PARSE_HTML)
    public void parseUrlBody(List<ConsumerRecord<String, RequestUrlValueDTO>> records, Acknowledgment ack) {
        List<RequestUrlValueDTO> collect1 = records.stream().map(ConsumerRecord::value).collect(Collectors.toList());
        List<ParseHtmlValueDTO> run = htmlParse.run(collect1);
        ack.acknowledge();
        for (ParseHtmlValueDTO parseHtmlValueDTO : run) {
            for (String url : parseHtmlValueDTO.getUrls()) {
                producer.sendMessageRequestUrl(KTopics.REQUEST_URL, RequestUrlDTO.builder().url(url).build());
            }
        }

    }
}

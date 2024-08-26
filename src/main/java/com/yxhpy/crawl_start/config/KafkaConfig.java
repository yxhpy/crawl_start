package com.yxhpy.crawl_start.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import javax.annotation.Resource;
import java.util.Map;

@Configuration
public class KafkaConfig {


    @Resource
    KafkaProperties kafkaProperties;

    @Bean
    public <T> ProducerFactory<String, T> producerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public <T> KafkaTemplate<String, T> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    public <T> ConsumerFactory<String, T> consumerFactory() {
        Map<String, Object> props = kafkaProperties.getConsumer().buildProperties();
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }


    @Bean
    public <T> ConcurrentKafkaListenerContainerFactory<String, T> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true); // 启用批量监听
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // 设置手动提交
        return factory;
    }
}

package com.yxhpy.crawl_start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(
        exclude = {
                MongoAutoConfiguration.class,
        }
)
public class CrawlStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrawlStartApplication.class, args);
    }

}

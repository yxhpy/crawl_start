package com.yxhpy.crawl_start.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestUrlValueDTO {
    private String url;
    private String html;
    private List<String> words;
    private Map<String, Integer> bagOfWords;
    private List<Object[]> topWords;
}

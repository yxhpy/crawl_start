package com.yxhpy.crawl_start.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParseHtmlValueDTO {
    private String url;
    private String html;
    private String title;
    private List<String> urls;
}

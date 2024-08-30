package com.yxhpy;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class WebContentExtractor {
    private static void extractText(Element element, StringBuilder textBuilder) {
        if (!element.ownText().isEmpty()) {
            textBuilder.append(element.ownText()).append(" ");
        }
        Elements children = element.children();
        for (Element child : children) {
            extractText(child, textBuilder);
        }
    }

    public static Map<String, String> extractContent(String html) {
        Map<String, String> content = new HashMap<>();

        Document doc = Jsoup.parse(html);
//        Document doc = Jsoup.connect(url).get();

        // 提取标题
        String title = doc.title();
        content.put("title", title);

        // 提取元数据
        Elements metaTags = doc.getElementsByTag("meta");
        for (Element metaTag : metaTags) {
            String name = metaTag.attr("name");
            String property = metaTag.attr("property");
            String content_attr = metaTag.attr("content");

            if (!name.isEmpty()) {
                content.put("meta_" + name, content_attr);
            } else if (!property.isEmpty()) {
                content.put("meta_" + property, content_attr);
            }
        }

        // 提取正文
        // 这里使用一个简单的启发式方法，可能需要根据具体网站结构调整
        StringBuilder textBuilder = new StringBuilder();
        // 提取 <body> 中的文本
        Element body = doc.body();
        extractText(body, textBuilder);
        String text = textBuilder.toString();
        text = text.replaceAll("[^\\p{L}\\p{N}\\s]", "");
        content.put("main_content", text);
        return content;
    }

}


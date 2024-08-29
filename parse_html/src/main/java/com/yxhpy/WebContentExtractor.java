package com.yxhpy;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebContentExtractor {

    public static Map<String, String> extractContent(String html) throws IOException {
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
        Elements paragraphs = doc.select("article p, div.content p, div.article-content p");
        if (paragraphs.isEmpty()) {
            paragraphs = doc.select("p");
        }
        StringBuilder mainContent = new StringBuilder();
        for (Element paragraph : paragraphs) {
            mainContent.append(paragraph.text()).append("\n");
        }
        content.put("main_content", mainContent.toString().trim());

        return content;
    }

    public static void main(String[] args) {
        try {
            String url = "https://example.com"; // 替换为你想要提取内容的网页 URL
            Map<String, String> extractedContent = extractContent(url);

            System.out.println("标题: " + extractedContent.get("title"));
            System.out.println("正文: " + extractedContent.get("main_content"));

            for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
                if (entry.getKey().startsWith("meta_")) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


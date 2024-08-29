package com.yxhpy;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.yxhpy.crawl_start.entity.RequestUrlDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import com.yxhpy.crawl_start.utils.UrlDownload;
import io.reactivex.rxjava3.core.Observable;

import java.util.*;


public class TextPreprocessor {

    private static final JiebaSegmenter segmenter = new JiebaSegmenter();
    private static final Set<String> stopWords = new HashSet<>(Arrays.asList(
            "的", "了", "和", "是", "就", "都", "而", "及", "与", "着"
            // 添加更多停用词...
    ));

    public static String removeHtmlTags(String html) {
        return html.replaceAll("<[^>]*>", "");
    }

    public static String removeSpecialCharacters(String text) {
        return text.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", " ").replaceAll("\\s+", " ").trim();
    }

    public static List<String> tokenize(String text) {
        List<SegToken> tokens = segmenter.process(text, JiebaSegmenter.SegMode.INDEX);
        List<String> words = new ArrayList<>();
        for (SegToken token : tokens) {
            String word = token.word.trim().toLowerCase();
            if (!word.isEmpty() && !stopWords.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    public static String preprocess(String text) {
        String noHtml = removeHtmlTags(text);
        String noSpecialChars = removeSpecialCharacters(noHtml);
        List<String> tokens = tokenize(noSpecialChars);
        return String.join(" ", tokens);
    }

    public static void main(String[] args) {
        Observable<RequestUrlValueDTO> request = new UrlDownload().request(RequestUrlDTO.builder().url("https://www.baidu.com").retryTimes(0).build());
        request.doOnNext(e -> {
                    String text = e.getHtml();
                    Map<String, String> stringStringMap = WebContentExtractor.extractContent(text);
                    System.out.println(stringStringMap);
//                    String preprocessed = preprocess(text);
//                    System.out.println("原文本: " + text);
//                    System.out.println("预处理后: " + preprocessed);
                })
                .subscribe();
    }
}


package com.yxhpy;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class TextPreprocessor {

    private static final Set<String> stopWords = new HashSet<>();

    static {
        // 读取停词到内存中
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources("classpath:stopwords/*");
            for (Resource resource : resources) {
                String s = new String(resource.getInputStream().readAllBytes());
                stopWords.addAll(Arrays.asList(s.split("\n")));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String removeHtmlTags(String html) {
        return html.replaceAll("<[^>]*>", "");
    }

    public static String removeSpecialCharacters(String text) {
        return text.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", " ").replaceAll("\\s+", " ").trim();
    }

    public static List<String> tokenize(String text) {
        List<Term> terms = HanLP.segment(text);
        List<String> words = new ArrayList<>();
        for (Term term : terms) {
            String word = term.word.trim();
            String posTag = term.nature.toString();
            if (!word.isEmpty() && !stopWords.contains(word)) {
                word = AdvancedHanLPLemmatization.simpleLemmatize(word, posTag);
                words.add(word);
            }
        }
        return words;
    }

    public static List<String> preprocess(String text) {
        String noHtml = removeHtmlTags(text);
        String noSpecialChars = removeSpecialCharacters(noHtml);
        return tokenize(noSpecialChars);
    }

    public static void main(String[] args) {
        List<String> words = preprocess("你好,你多少岁了呀？我今年20岁了,我买了iphone13");
        System.out.println(words);
    }
}


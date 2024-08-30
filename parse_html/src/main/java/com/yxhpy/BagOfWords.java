package com.yxhpy;

import java.util.*;

public class BagOfWords {
    public static Map<String, Integer> createBagOfWords(String text) {
        // 将文本转换为小写并分割成单词
        String[] words = text.toLowerCase().split("\\W+");

        // 创建一个Map来存储词频
        Map<String, Integer> bagOfWords = new HashMap<>();

        // 统计每个词的频率
        for (String word : words) {
            bagOfWords.put(word, bagOfWords.getOrDefault(word, 0) + 1);
        }

        return bagOfWords;
    }
}
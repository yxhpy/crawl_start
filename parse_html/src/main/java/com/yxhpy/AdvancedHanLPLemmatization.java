package com.yxhpy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdvancedHanLPLemmatization {

    private static final Set<String> TIME_SUFFIXES = new HashSet<>(Arrays.asList("年", "月", "日", "时", "分", "秒"));
    private static final Set<String> MEASURE_WORDS = new HashSet<>(Arrays.asList("个", "只", "张", "本", "双", "对", "幅", "面"));

    public static String simpleLemmatize(String word, String posTag) {
        // 处理动词
        if (posTag.startsWith("v")) {
            return lemmatizeVerb(word);
        }
        // 处理形容词
        else if (posTag.startsWith("a")) {
            return lemmatizeAdjective(word);
        }
        // 处理名词
        else if (posTag.startsWith("n")) {
            return lemmatizeNoun(word);
        }
        // 处理副词
        else if (posTag.startsWith("d")) {
            return lemmatizeAdverb(word);
        }
        // 处理数词和量词
        else if (posTag.equals("m") || posTag.equals("q")) {
            return lemmatizeNumeral(word);
        }
        // 其他情况，返回原词
        return word;
    }

    private static String lemmatizeVerb(String word) {
        // 去除常见的动词后缀
        if (word.endsWith("了") || word.endsWith("过") || word.endsWith("着")) {
            return word.substring(0, word.length() - 1);
        }
        // 处理"不"字前缀
        if (word.startsWith("不") && word.length() > 1) {
            return word.substring(1);
        }
        // 处理重叠型动词，如"看看"、"说说"
        if (word.length() == 2 && word.charAt(0) == word.charAt(1)) {
            return word.substring(0, 1);
        }
        return word;
    }

    private static String lemmatizeAdjective(String word) {
        // 去除程度副词前缀
        String[] prefixes = {"很", "非常", "极其", "格外", "分外", "更加", "最"};
        for (String prefix : prefixes) {
            if (word.startsWith(prefix)) {
                return word.substring(prefix.length());
            }
        }
        // 处理重叠型形容词，如"红红的"
        if (word.length() == 3 && word.charAt(0) == word.charAt(1) && word.charAt(2) == '的') {
            return word.substring(0, 1);
        }
        return word;
    }

    private static String lemmatizeNoun(String word) {
        // 去除常见的名词后缀
        String[] suffixes = {"们", "子", "家"};
        for (String suffix : suffixes) {
            if (word.endsWith(suffix)) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        // 处理时间相关的名词
        for (String timeSuffix : TIME_SUFFIXES) {
            if (word.endsWith(timeSuffix) && word.length() > timeSuffix.length()) {
                return word.substring(0, word.length() - timeSuffix.length());
            }
        }
        return word;
    }

    private static String lemmatizeAdverb(String word) {
        // 去除"地"字后缀
        if (word.endsWith("地") && word.length() > 1) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private static String lemmatizeNumeral(String word) {
        // 去除量词
        for (String measureWord : MEASURE_WORDS) {
            if (word.endsWith(measureWord)) {
                return word.substring(0, word.length() - measureWord.length());
            }
        }
        return word;
    }
}

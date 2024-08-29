package com.yxhpy.crawl_start.utils;

import com.yxhpy.crawl_start.entity.ParseHtmlValueDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


@Component
public class HtmlParse {


    public Observable<ParseHtmlValueDTO> parseHtml(RequestUrlValueDTO parseHtml) {
        return Observable.create((emitter -> {
            try {
                ParseHtmlValueDTO parseHtmlValueDTO = new ParseHtmlValueDTO();
                BeanUtils.copyProperties(parseHtml, parseHtmlValueDTO);

                Set<String> links = new HashSet<>(); // 使用 Set 来自动去重
                Document doc = Jsoup.parse(parseHtml.getHtml());
                Elements elements = doc.select("a[href]");
                Elements title = doc.select("title");
                String text = title.text();
                for (Element element : elements) {
                    String href = element.attr("href");
                    // 使用正则表达式匹配形如 "https://www.example.com" 的链接
                    if (href.matches("https?://[\\w.-]+\\.[a-zA-Z]{2,}")) {
                        try {
                            URL url = new URL(href);
                            // 只保留协议和主机名部分
                            String baseUrl = url.getProtocol() + "://" + url.getHost();
                            links.add(baseUrl);
                        } catch (MalformedURLException e) {
                            // 忽略无效的 URL
                        }
                    }
                }
                parseHtmlValueDTO.setUrls(new ArrayList<>(links)); // 将 Set 转换为 List
                parseHtmlValueDTO.setTitle(text);
                emitter.onNext(parseHtmlValueDTO);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }));
    }

    public List<ParseHtmlValueDTO> run(List<RequestUrlValueDTO> htmlList) {
        List<ParseHtmlValueDTO> results = new CopyOnWriteArrayList<>();
        Disposable subscribe = Observable.fromIterable(htmlList)
                .subscribeOn(Schedulers.computation())
                .flatMap(this::parseHtml)
                .observeOn(Schedulers.computation())
                .subscribe(results::add);
        while (!subscribe.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return results;
    }
}

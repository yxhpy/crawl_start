package com.yxhpy.crawl_start.utils;

import com.yxhpy.crawl_start.entity.ParseHtmlValueDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Synchronized;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Component
public class UrlDownload {
    static OkHttpClient client = null;

    public Observable<RequestUrlValueDTO> request(String url) {
        return Observable.create((emitter -> {
            Request.Builder builder = new Request.Builder();
            Request request = builder.url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .get().build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    try (response; response) {
                        ResponseBody body = response.body();
                        if (body != null) {
                            RequestUrlValueDTO build = RequestUrlValueDTO.builder()
                                    .html(body.string())
                                    .url(url)
                                    .build();
                            emitter.onNext(build);
                            emitter.onComplete();
                        }
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    emitter.onError(e);
                }
            });
        }));
    }

    public Observable<RequestUrlValueDTO> requestAndRetry(String url) {
        return request(url)
                .retry(2)
                .onErrorResumeNext(Observable::error);
    }

    @Synchronized
    public List<RequestUrlValueDTO> run(List<String> urls) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        client = builder
                .callTimeout(10, TimeUnit.SECONDS)
                .build();
        List<RequestUrlValueDTO> results = new CopyOnWriteArrayList<>();
        Disposable completed = Observable.fromIterable(urls)
                .subscribeOn(Schedulers.io())
                .flatMap(this::requestAndRetry, true)
                .observeOn(Schedulers.computation())
                .subscribe((results::add), e -> {
                    System.out.println("异常");
                }, () -> {
                    System.out.println("Completed");
                });
        while (!completed.isDisposed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        return results;
    }

    public static void main(String[] args) {
        UrlDownload urlDownload = new UrlDownload();
        List<String> collect = List.of(
                "https://www.hao123.com",
                "https://www.hao123.com1",
                "https://www.hao123.com"
        );
        while (!collect.isEmpty()) {
            List<RequestUrlValueDTO> run = urlDownload.run(collect);
            HtmlParse htmlParse = new HtmlParse();
            List<ParseHtmlValueDTO> run1 = htmlParse.run(run);
            for (ParseHtmlValueDTO parseHtmlValueDTO : run1) {
                System.out.printf("%s %s\n", parseHtmlValueDTO.getTitle(), parseHtmlValueDTO.getUrl());
            }
            collect = run1.stream().map(ParseHtmlValueDTO::getUrls).flatMap(List::stream).collect(Collectors.toList());
            collect = collect.subList(0, Math.min(collect.size(), 100));
            System.out.println(collect.size());
        }
    }
}

package com.yxhpy.crawl_start.utils;

import com.yxhpy.crawl_start.entity.ParseHtmlValueDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UrlDownload {
    static OkHttpClient client = null;

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        client = builder
                .callTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    private final Semaphore semaphore = new Semaphore(10); // 限制并发请求数

    public Observable<RequestUrlValueDTO> request(String url) {
        return Observable.create((emitter -> {
            try {
                semaphore.acquire();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url(url)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                        .get().build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try (ResponseBody body = response.body()) {
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
                        } finally {
                            semaphore.release();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        emitter.onError(e);
                        semaphore.release();
                    }
                });
            } catch (InterruptedException e) {
                emitter.onError(e);
            }
        }));
    }

    public Observable<RequestUrlValueDTO> requestAndRetry(String url) {
        return request(url)
                .retryWhen(errors -> errors.zipWith(Observable.range(1, 3), (n, i) -> {
                            log.debug("访问第{}次，发生错误:{}", i, n.getMessage());
                            if (i >= 3) {
                                throw n;
                            }
                            return i;
                        })
                        .flatMap(i -> Observable.timer((long) Math.pow(2, i), TimeUnit.SECONDS)))
                .onErrorResumeNext(throwable -> {
                    log.error("Error after retries for URL {}: {}", url, throwable.getMessage());
                    return Observable.empty(); // 返回空 Observable 而不是错误
                });
    }


    @Synchronized
    public List<RequestUrlValueDTO> run(List<String> urls) {
        List<RequestUrlValueDTO> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(urls.size());
        Observable.fromIterable(urls)
                .subscribeOn(Schedulers.io())
                .flatMap((url) -> this.requestAndRetry(url).doFinally(latch::countDown))
                .observeOn(Schedulers.computation())
                .subscribe((results::add));
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation was interrupted", e);
        }
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
                log.debug("{} {}", parseHtmlValueDTO.getTitle(), parseHtmlValueDTO.getUrl());
            }
            collect = run1.stream().map(ParseHtmlValueDTO::getUrls).flatMap(List::stream).collect(Collectors.toList());
            collect = collect.subList(0, Math.min(collect.size(), 500));
            log.debug("获得子网站{}个", collect.size());
        }
    }
}

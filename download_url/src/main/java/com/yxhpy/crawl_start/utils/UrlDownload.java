package com.yxhpy.crawl_start.utils;

import com.yxhpy.crawl_start.entity.RequestUrlDTO;
import com.yxhpy.crawl_start.entity.RequestUrlValueDTO;
import com.yxhpy.crawl_start.kconst.KTopics;
import com.yxhpy.crawl_start.producer.Producer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UrlDownload {
    static OkHttpClient client = null;
    @Resource
    Producer producer;

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        client = builder
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    private final Semaphore finalSemaphore = new Semaphore(100); // 限制并发请求数
    private final Semaphore finalSemaphoreRetry = new Semaphore(10); // 限制并发请求数


    private Semaphore getSemaphore(RequestUrlDTO requestUrlDTO) {
        if (requestUrlDTO.getRetryTimes() == 0) {
            return finalSemaphore;
        } else {
            return finalSemaphoreRetry;
        }
    }

    public Observable<RequestUrlValueDTO> request(RequestUrlDTO requestUrlDTO) {
        Semaphore semaphore = getSemaphore(requestUrlDTO);
        String url = requestUrlDTO.getUrl();
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

    public Observable<RequestUrlValueDTO> requestAndRetry(RequestUrlDTO url) {
        return request(url)
                .doOnError(throwable -> {
                    if (url.getRetryTimes() < 3) {
                        producer.sendMessageRetryRequestUrl(KTopics.RETRY_PARSE_HTML, url);
                    }
                })
                .onErrorResumeNext(throwable -> {
                    log.debug("Error after retries for URL {}: {}", url, throwable.getMessage());
                    return Observable.empty(); // 返回空 Observable 而不是错误
                });
    }


    public List<RequestUrlValueDTO> run(List<RequestUrlDTO> urls) {
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

//    public static void main(String[] args) {
//        UrlDownload urlDownload = new UrlDownload();
//        List<String> collect = List.of(
//                "https://www.hao123.com"
//        );
//        while (!collect.isEmpty()) {
//            if (collect.size() == 100) {
//                collect = collect.stream().map(i -> (new Random().nextBoolean() ? i : (i + ""))).collect(Collectors.toList());
//            }
//            long start = System.currentTimeMillis();
//            List<RequestUrlValueDTO> run = urlDownload.run(collect);
//            System.out.println(System.currentTimeMillis() - start);
//            HtmlParse htmlParse = new HtmlParse();
//            List<ParseHtmlValueDTO> run1 = htmlParse.run(run);
//            for (ParseHtmlValueDTO parseHtmlValueDTO : run1) {
//                log.debug("{} {}", parseHtmlValueDTO.getTitle(), parseHtmlValueDTO.getUrl());
//            }
//            collect = run1.stream().map(ParseHtmlValueDTO::getUrls).flatMap(List::stream).collect(Collectors.toList());
//            collect = collect.subList(0, Math.min(collect.size(), 100));
//            log.debug("获得子网站{}个", collect.size());
//        }
//    }
}

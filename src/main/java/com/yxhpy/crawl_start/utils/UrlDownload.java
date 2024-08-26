package com.yxhpy.crawl_start.utils;

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
                    if (!emitter.isDisposed()) {
                        emitter.onError(new Exception(e.getMessage()));
                    }
                }
            });
        }));
    }

    public Observable<RequestUrlValueDTO> requestAndRetry(String url) {
        return request(url)
                .retry(3)
                .timeout(10, TimeUnit.SECONDS) // 设置 30 秒超时
                .onErrorResumeNext(Observable::error);
    }

    @Synchronized
    public List<RequestUrlValueDTO> run(List<String> urls) {
        client = new OkHttpClient();
        List<RequestUrlValueDTO> results = new CopyOnWriteArrayList<>();
        Disposable completed = Observable.fromIterable(urls)
                .subscribeOn(Schedulers.io())
                .flatMap(this::requestAndRetry)
                .observeOn(Schedulers.computation())
                .subscribe((results::add), e -> {
                    System.out.println(e.getMessage());
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
}

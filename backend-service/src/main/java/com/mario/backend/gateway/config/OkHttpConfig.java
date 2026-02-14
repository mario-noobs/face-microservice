package com.mario.backend.gateway.config;

import com.mario.backend.face.service.IdempotencyService;
import com.mario.backend.logging.context.TraceContext;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Value("${http-client.connect-timeout:30000}")
    private long connectTimeout;

    @Value("${http-client.read-timeout:60000}")
    private long readTimeout;

    @Value("${http-client.write-timeout:60000}")
    private long writeTimeout;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .addInterceptor(chain -> {
                    okhttp3.Request.Builder builder = chain.request().newBuilder();
                    String traceId = TraceContext.getTraceId();
                    if (traceId != null) {
                        builder.header("X-Request-ID", traceId);
                    }
                    String idempotencyKey = IdempotencyService.getCurrentKey();
                    if (idempotencyKey != null) {
                        builder.header("X-Idempotency-Key", idempotencyKey);
                    }
                    return chain.proceed(builder.build());
                })
                .build();
    }
}

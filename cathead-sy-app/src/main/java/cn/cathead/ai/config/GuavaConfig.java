package cn.cathead.ai.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Configuration
public class GuavaConfig {
    @Bean(name = "chatModelCache")
    public Cache<String, ChatModel> ChatmodelCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(50)
                .build();
    }

    @Bean(name = "embeddingModelCache")
    public Cache<String, EmbeddingModel> EmbeddingModelCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(50)
                .build();
    }
}

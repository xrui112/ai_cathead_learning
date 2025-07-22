package cn.cathead.ai.test.config;

import cn.cathead.ai.domain.model.model.entity.ModelWrapper;
import cn.cathead.ai.domain.model.service.provider.IModelProvider;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 测试配置类
 */
@Configuration
public class TestConfiguration {

    /**
     * 测试用Chat模型缓存
     */
    @Bean("chatModelCache")
    @Primary
    public Cache<String, ModelWrapper<ChatModel>> testChatModelCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * 测试用Embedding模型缓存
     */
    @Bean("embeddingModelCache")
    @Primary
    public Cache<String, ModelWrapper<EmbeddingModel>> testEmbeddingModelCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
} 
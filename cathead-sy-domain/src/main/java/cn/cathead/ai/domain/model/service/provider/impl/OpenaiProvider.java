package cn.cathead.ai.domain.model.service.provider.impl;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.valobj.ModelPropertyVo;
import cn.cathead.ai.domain.model.service.provider.IModelProvider;
import cn.cathead.ai.types.utils.ReflectionUtils;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("openai")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OpenaiProvider implements IModelProvider {
    
    @Override
    public ChatModel createChat(ChatModelEntity chatModelEntity) {
         OpenAiApi openAiApi= OpenAiApi
                 .builder()
                .baseUrl(chatModelEntity.getUrl())
                .apiKey(chatModelEntity.getKey())
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions
                .builder()
                .model(chatModelEntity.getModelName())
                .temperature(chatModelEntity.getTemperature() == null
                        ? Double.parseDouble(ModelPropertyVo.TEMPERATURE.getDefaultValue())
                        : chatModelEntity.getTemperature())
                .topP(chatModelEntity.getTopP() == null
                        ? Double.parseDouble(ModelPropertyVo.TOP_K.getDefaultValue())
                        : chatModelEntity.getTopP())
                .maxTokens(chatModelEntity.getMaxTokens() == null
                        ? Integer.parseInt(ModelPropertyVo.MAX_TOKENS.getDefaultValue())
                        : chatModelEntity.getMaxTokens())
                .stop(chatModelEntity.getStop() == null || chatModelEntity.getStop().length == 0
                        ? List.of(ModelPropertyVo.STOP.getDefaultArray())
                        : List.of(chatModelEntity.getStop()))
                .frequencyPenalty(chatModelEntity.getFrequencyPenalty() == null
                        ? Double.parseDouble(ModelPropertyVo.FREQUENCY_PENALTY.getDefaultValue())
                        : chatModelEntity.getFrequencyPenalty())
                .presencePenalty(chatModelEntity.getPresencePenalty() == null
                        ? Double.parseDouble(ModelPropertyVo.PRESENCE_PENALTY.getDefaultValue())
                        : chatModelEntity.getPresencePenalty());

        // 处理动态属性
        if (chatModelEntity.getDynamicProperties() != null && !chatModelEntity.getDynamicProperties().isEmpty()) {
            applyDynamicProperties(optionsBuilder, chatModelEntity.getDynamicProperties());
        }

        OpenAiChatOptions options = optionsBuilder.build();

        return new OpenAiChatModel(
                openAiApi,
                options,
                ToolCallingManager.builder().build(),
                RetryTemplate.defaultInstance(),
                ObservationRegistry.NOOP
        );
    }

    @Override
    public EmbeddingModel createEmbedding(EmbeddingModelEntity embeddingModelEntity) {
        OpenAiApi client = OpenAiApi.builder()
                .baseUrl(embeddingModelEntity.getUrl())
                .apiKey(embeddingModelEntity.getKey())
                .build();

        MetadataMode metadataMode=MetadataMode.ALL;
        OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelEntity.getModelName());

        // 处理动态属性
        if (embeddingModelEntity.getDynamicProperties() != null && !embeddingModelEntity.getDynamicProperties().isEmpty()) {
            applyEmbeddingDynamicProperties(optionsBuilder, embeddingModelEntity.getDynamicProperties());
        }

        OpenAiEmbeddingOptions options = optionsBuilder.build();
        return new OpenAiEmbeddingModel(client, metadataMode, options);
    }
    
    /**
     * 通过反射设置动态属性到Embedding Options Builder
     */
    private void applyEmbeddingDynamicProperties(OpenAiEmbeddingOptions.Builder optionsBuilder, Map<String, Object> dynamicProperties) {
        for (Map.Entry<String, Object> entry : dynamicProperties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();
            
            try {
                // 尝试找到对应的setter方法
                Method method = ReflectionUtils.findSetterMethod(optionsBuilder.getClass(), propertyName, value);
                if (method != null) {
                    method.invoke(optionsBuilder, ReflectionUtils.convertValue(value, method.getParameterTypes()[0]));
                    log.info("成功设置OpenAI Embedding动态属性: {} = {}", propertyName, value);
                } else {
                    log.warn("未找到OpenAI Embedding属性的setter方法: {}", propertyName);
                }
            } catch (Exception e) {
                log.error("设置OpenAI Embedding动态属性失败: {} = {}, 错误: {}", propertyName, value, e.getMessage());
            }
        }
    }
    
    /**
     * 通过反射设置动态属性到Chat Options Builder
     */
    private void applyDynamicProperties(OpenAiChatOptions.Builder optionsBuilder, Map<String, Object> dynamicProperties) {
        for (Map.Entry<String, Object> entry : dynamicProperties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();
            
            try {
                // 尝试找到对应的setter方法
                Method method = ReflectionUtils.findSetterMethod(optionsBuilder.getClass(), propertyName, value);
                if (method != null) {
                    method.invoke(optionsBuilder, ReflectionUtils.convertValue(value, method.getParameterTypes()[0]));
                    log.info("成功设置OpenAI动态属性: {} = {}", propertyName, value);
                } else {
                    log.warn("未找到OpenAI属性的setter方法: {}", propertyName);
                }
            } catch (Exception e) {
                log.error("设置OpenAI动态属性失败: {} = {}, 错误: {}", propertyName, value, e.getMessage());
            }
        }
    }
    

}

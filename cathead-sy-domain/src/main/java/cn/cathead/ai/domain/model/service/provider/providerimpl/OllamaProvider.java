package cn.cathead.ai.domain.model.service.provider.providerimpl;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.valobj.ModelPropertyVo;
import cn.cathead.ai.domain.model.service.provider.IModelProvider;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("ollama")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class OllamaProvider implements IModelProvider {

    //初次创建
    @Override
    public OllamaChatModel createChat(ChatModelEntity chatModelEntity) {
        OllamaApi ollamaApi = new OllamaApi.Builder()
                .baseUrl(chatModelEntity.getUrl())
                .build();

        OllamaOptions.Builder optionsBuilder = OllamaOptions.builder()
                .model(chatModelEntity.getModelName())
                .temperature(Double.valueOf(chatModelEntity.getTemperature() == null
                        ? Float.valueOf(ModelPropertyVo.TEMPERATURE.getDefaultValue())
                        : chatModelEntity.getTemperature()))
                .topP(Double.valueOf(chatModelEntity.getTopP() == null
                        ? Float.valueOf(ModelPropertyVo.TOP_K.getDefaultValue())
                        : chatModelEntity.getTopP()))
                .numPredict(chatModelEntity.getMaxTokens() == null
                        ? Integer.valueOf(ModelPropertyVo.MAX_TOKENS.getDefaultValue())
                        : chatModelEntity.getMaxTokens())
                .stop(List.of(chatModelEntity.getStop() == null || chatModelEntity.getStop().length == 0
                        ? ModelPropertyVo.STOP.getDefaultArray()
                        : chatModelEntity.getStop()))
                .frequencyPenalty(Double.valueOf(chatModelEntity.getFrequencyPenalty() == null
                        ? Float.valueOf(ModelPropertyVo.FREQUENCY_PENALTY.getDefaultValue())
                        : chatModelEntity.getFrequencyPenalty()))
                .presencePenalty(Double.valueOf(chatModelEntity.getPresencePenalty() == null
                        ? Float.valueOf(ModelPropertyVo.PRESENCE_PENALTY.getDefaultValue())
                        : chatModelEntity.getPresencePenalty()));

        // 处理动态属性
        if (chatModelEntity.getDynamicProperties() != null && !chatModelEntity.getDynamicProperties().isEmpty()) {
            applyDynamicProperties(optionsBuilder, chatModelEntity.getDynamicProperties());
        }

        return new OllamaChatModel(
                ollamaApi,
                optionsBuilder.build(),
                ToolCallingManager
                        .builder()
                        .build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions
                        .builder()
                        .build()
        );
    }

    @Override
    public OllamaEmbeddingModel createEmbedding(EmbeddingModelEntity embeddingModelEntity) {
        OllamaApi ollamaApi = new OllamaApi.Builder()
                .baseUrl(embeddingModelEntity.getUrl())
                .build(); // Ollama 也不需要 key

        OllamaOptions.Builder optionsBuilder = OllamaOptions.builder()
                .model(embeddingModelEntity.getModelName())
                .format(embeddingModelEntity.getEmbeddingFormat() == null
                        ? ModelPropertyVo.EMBEDDIDNGFORMAT.getDefaultValue()
                        : embeddingModelEntity.getEmbeddingFormat())
                .numPredict(embeddingModelEntity.getNumPredict() == null
                        ? Integer.valueOf(ModelPropertyVo.NUMPREDICT.getDefaultValue())
                        : embeddingModelEntity.getNumPredict());

        // 处理动态属性
        if (embeddingModelEntity.getDynamicProperties() != null && !embeddingModelEntity.getDynamicProperties().isEmpty()) {
            applyDynamicProperties(optionsBuilder, embeddingModelEntity.getDynamicProperties());
        }

        return new OllamaEmbeddingModel(
                ollamaApi,
                optionsBuilder.build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.builder().build()
        );
    }

    /**
     * 通过反射设置动态属性到Options Builder
     * @param optionsBuilder Options Builder对象
     * @param dynamicProperties 动态属性Map
     */
    private void applyDynamicProperties(OllamaOptions.Builder optionsBuilder, Map<String, Object> dynamicProperties) {
        for (Map.Entry<String, Object> entry : dynamicProperties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();
            
            try {
                // 尝试找到对应的setter方法
                Method method = findSetterMethod(optionsBuilder.getClass(), propertyName, value);
                if (method != null) {
                    method.invoke(optionsBuilder, convertValue(value, method.getParameterTypes()[0]));
                    log.info("成功设置Ollama动态属性: {} = {}", propertyName, value);
                } else {
                    log.warn("未找到Ollama属性的setter方法: {}", propertyName);
                }
            } catch (Exception e) {
                log.error("设置Ollama动态属性失败: {} = {}, 错误: {}", propertyName, value, e.getMessage());
            }
        }
    }
    
    /**
     * 查找setter方法
     */
    private Method findSetterMethod(Class<?> clazz, String propertyName, Object value) {
        String methodName = propertyName;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        
        return null;
    }
    
    /**
     * 转换值到目标类型
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // 基本类型转换
        if (targetType == Double.class || targetType == double.class) {
            return ((Number) value).doubleValue();
        } else if (targetType == Float.class || targetType == float.class) {
            return ((Number) value).floatValue();
        } else if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        } else if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        } else if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value.toString());
        }
        
        return value;
    }
}

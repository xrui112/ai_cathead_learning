package cn.cathead.ai.domain.model.service.update.impl;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.service.update.AbstractModelUpdateTemplate;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.utils.FormDataConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Chat模型更新服务
 * 实现Chat模型的更新逻辑
 */
@Slf4j
@Service
public class ChatModelUpdateService extends AbstractModelUpdateTemplate<ChatModelEntity, ChatModelDTO> {

    @Override
    protected String getModelType() {
        return "chat";
    }

    @Override
    protected ChatModelEntity buildUpdatedEntity(String modelId, ChatModelDTO updateData, Long version) {
        return ChatModelEntity.builder()
                .modelId(modelId)
                .providerName(updateData.getProviderName())
                .modelName(updateData.getModelName())
                .url(updateData.getUrl())
                .key(updateData.getKey())
                .type(updateData.getType())
                .temperature(updateData.getTemperature())
                .topP(updateData.getTopP())
                .maxTokens(updateData.getMaxTokens())
                .presencePenalty(updateData.getPresencePenalty())
                .frequencyPenalty(updateData.getFrequencyPenalty())
                .stop(updateData.getStop())
                .version(version)
                .build();
    }

    @Override
    protected ChatModelEntity buildEntityFromFormData(String modelId, String provider, Map<String, Object> formData, Long version) {
        // 注意：传入的formData已经通过applyDefaultValues处理过，包含默认值，可以直接使用FormDataConverter的基础方法
        
        // 标准字段
        ChatModelEntity.ChatModelEntityBuilder<?, ?> builder = ChatModelEntity.builder()
                .modelId(modelId)
                .providerName(provider)
                .modelName(FormDataConverter.getStringValue(formData, "modelName"))
                .url(FormDataConverter.getStringValue(formData, "url"))
                .key(FormDataConverter.getStringValue(formData, "key"))
                .type(getModelType())
                .temperature(FormDataConverter.getFloatValue(formData, "temperature"))
                .topP(FormDataConverter.getFloatValue(formData, "topP"))
                .maxTokens(FormDataConverter.getIntegerValue(formData, "maxTokens"))
                .presencePenalty(FormDataConverter.getFloatValue(formData, "presencePenalty"))
                .frequencyPenalty(FormDataConverter.getFloatValue(formData, "frequencyPenalty"))
                .stop(FormDataConverter.getStringArrayValue(formData, "stop"))
                .version(version);

        // 动态属性：除了标准字段外的其他字段
        Map<String, Object> dynamicProperties = FormDataConverter.extractDynamicProperties(formData,
                "modelName", "url", "key", "temperature", "topP", "maxTokens",
                "presencePenalty", "frequencyPenalty", "stop");

        return builder.dynamicProperties(dynamicProperties).build();
    }

    @Override
    protected void updateCache(String modelId, ChatModelEntity entity) {
        modelCacheManager.updateChatModelBean(modelId, entity);
    }
}
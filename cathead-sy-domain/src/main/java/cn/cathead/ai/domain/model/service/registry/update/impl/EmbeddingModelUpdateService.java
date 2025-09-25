package cn.cathead.ai.domain.model.service.registry.update.impl;

import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.service.registry.update.AbstractModelUpdateTemplate;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import cn.cathead.ai.types.utils.FormDataConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Embedding模型更新服务
 * 实现Embedding模型的更新逻辑
 */
@Slf4j
@Service
public class EmbeddingModelUpdateService extends AbstractModelUpdateTemplate<EmbeddingModelEntity, EmbeddingModelDTO> {

    @Override
    protected String getModelType() {
        return "embedding";
    }

    @Override
    protected EmbeddingModelEntity buildUpdatedEntity(String modelId, EmbeddingModelDTO updateData, Long version) {
        return EmbeddingModelEntity.builder()
                .modelId(modelId)
                .providerName(updateData.getProviderName())
                .modelName(updateData.getModelName())
                .url(updateData.getUrl())
                .key(updateData.getKey())
                .type(updateData.getType())
                .embeddingFormat(updateData.getEmbeddingFormat())
                .numPredict(updateData.getNumPredict())
                .dimensions(updateData.getDimensions())
                .version(version)
                .build();
    }

    @Override
    protected EmbeddingModelEntity buildEntityFromFormData(String modelId, String provider, Map<String, Object> formData, Long version) {
        // 注意：传入的formData已经通过applyDefaultValues处理过，包含默认值，可以直接使用FormDataConverter的基础方法
        
        // 标准字段
        EmbeddingModelEntity.EmbeddingModelEntityBuilder<?, ?> builder = EmbeddingModelEntity.builder()
                .modelId(modelId)
                .providerName(provider)
                .modelName(FormDataConverter.getStringValue(formData, "modelName"))
                .url(FormDataConverter.getStringValue(formData, "url"))
                .key(FormDataConverter.getStringValue(formData, "key"))
                .type(getModelType())
                .embeddingFormat(FormDataConverter.getStringValue(formData, "embeddingFormat"))
                .numPredict(FormDataConverter.getIntegerValue(formData, "numPredict"))
                .version(version);

        // 动态属性：除了标准字段外的其他字段
        Map<String, Object> dynamicProperties = FormDataConverter.extractDynamicProperties(formData,
                "modelName", "url", "key", "embeddingFormat", "numPredict");

        return builder.dynamicProperties(dynamicProperties).build();
    }

    @Override
    protected void updateCache(String modelId, EmbeddingModelEntity entity) {
        modelCacheManager.updateEmbeddingModelBean(modelId, entity);
    }
}
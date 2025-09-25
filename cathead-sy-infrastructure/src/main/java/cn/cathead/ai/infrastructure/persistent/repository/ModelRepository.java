package cn.cathead.ai.infrastructure.persistent.repository;

import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatRequestEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.ModelValidationRuleEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.infrastructure.persistent.dao.IModelDao;
import cn.cathead.ai.infrastructure.persistent.dao.IValidationRuleDao;
import cn.cathead.ai.infrastructure.persistent.po.ModelConfig;
import cn.cathead.ai.infrastructure.persistent.po.ChatRequest;
import cn.cathead.ai.infrastructure.persistent.po.ModelValidationRulePO;
import cn.cathead.ai.types.utils.JsonUtils;
import cn.cathead.ai.types.exception.OptimisticLockException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class ModelRepository implements IModelRepository {


    @Resource
    private IModelDao iModelDao;

    @Resource
    private IValidationRuleDao validationRuleDao;


    @Override
    public long saveModelRecord(BaseModelEntity baseModelEntity) {
        long version;
        if ("chat".equalsIgnoreCase(baseModelEntity.getType())){

            ChatModelEntity chatModelEntity=(ChatModelEntity) baseModelEntity;
            ModelConfig modelConfig =new ModelConfig();
            modelConfig.setProviderName(chatModelEntity.getProviderName());
            modelConfig.setModelId(chatModelEntity.getModelId());
            modelConfig.setModelName(chatModelEntity.getModelName());
            modelConfig.setUrl(chatModelEntity.getUrl());
            modelConfig.setKey(chatModelEntity.getKey());
            modelConfig.setType(chatModelEntity.getType());
            modelConfig.setTemperature(chatModelEntity.getTemperature());
            modelConfig.setTopP(chatModelEntity.getTopP());
            modelConfig.setMaxTokens(chatModelEntity.getMaxTokens());
            modelConfig.setStop(Arrays.toString(chatModelEntity.getStop()));
            modelConfig.setFrequencyPenalty(chatModelEntity.getFrequencyPenalty());
            modelConfig.setPresencePenalty(chatModelEntity.getPresencePenalty());
            modelConfig.setSystemPrompt(chatModelEntity.getSystemPrompt());
            modelConfig.setMaxContextLength(chatModelEntity.getMaxContextLength());
            modelConfig.setSupportStream(chatModelEntity.getSupportStream());
            modelConfig.setSupportFunctionCall(chatModelEntity.getSupportFunctionCall());
            // 转换动态属性为JSON字符串
            modelConfig.setDynamicProperties(JsonUtils.mapToJson(chatModelEntity.getDynamicProperties()));
            version=iModelDao.saveModelRecord(modelConfig);

        }else {
            EmbeddingModelEntity embeddingModelEntity=(EmbeddingModelEntity) baseModelEntity;
            ModelConfig modelConfig =new ModelConfig();
            modelConfig.setProviderName(embeddingModelEntity.getProviderName());
            modelConfig.setModelId(embeddingModelEntity.getModelId());
            modelConfig.setModelName(embeddingModelEntity.getModelName());
            modelConfig.setUrl(embeddingModelEntity.getUrl());
            modelConfig.setKey(embeddingModelEntity.getKey());
            modelConfig.setType(embeddingModelEntity.getType());
            modelConfig.setEmbeddingFormat(embeddingModelEntity.getEmbeddingFormat());
            modelConfig.setNumPredict(embeddingModelEntity.getNumPredict());
            modelConfig.setDimensions(embeddingModelEntity.getDimensions());
            modelConfig.setMaxInputLength(embeddingModelEntity.getMaxInputLength());
            modelConfig.setSupportBatch(embeddingModelEntity.getSupportBatch());
            modelConfig.setMaxBatchSize(embeddingModelEntity.getMaxBatchSize());
            modelConfig.setNormalize(embeddingModelEntity.getNormalize());
            modelConfig.setSimilarityMetric(embeddingModelEntity.getSimilarityMetric());
            // 转换动态属性为JSON字符串
            modelConfig.setDynamicProperties(JsonUtils.mapToJson(embeddingModelEntity.getDynamicProperties()));
            version=iModelDao.saveModelRecord(modelConfig);

        }
        return version;
    }

    // ========================= 规则表操作（与 IValidationRuleDao 对应） =========================

    public List<ModelValidationRuleEntity> findEnabledRules(String providerName, String modelType) {
        List<ModelValidationRulePO> list = validationRuleDao.selectEnabledRules(providerName, modelType);
        return list.stream().map(this::toRuleEntity).collect(Collectors.toList());
    }

    public List<ModelValidationRuleEntity> listRules(String providerName, String modelType) {
        List<ModelValidationRulePO> list = validationRuleDao.selectRules(providerName, modelType);
        return list.stream().map(this::toRuleEntity).collect(Collectors.toList());
    }

    public Long createRule(ModelValidationRuleEntity rule) {
        ModelValidationRulePO po = toRulePo(rule);
        validationRuleDao.insertRule(po);
        return po.getId();
    }

    public void updateRule(ModelValidationRuleEntity rule) {
        validationRuleDao.updateRule(toRulePo(rule));
    }

    public void deleteRule(Long id) {
        validationRuleDao.deleteRule(id);
    }

    private ModelValidationRuleEntity toRuleEntity(ModelValidationRulePO po) {
        return ModelValidationRuleEntity.builder()
                .id(po.getId())
                .providerName(po.getProviderName())
                .modelType(po.getModelType())
                .fieldName(po.getFieldName())
                .fieldType(po.getFieldType())
                .required(po.getRequired())
                .defaultValue(po.getDefaultValue())
                .minValue(po.getMinValue())
                .maxValue(po.getMaxValue())
                .minLength(po.getMinLength())
                .maxLength(po.getMaxLength())
                .pattern(po.getPattern())
                .enumValues(po.getEnumValues())
                .customValidator(po.getCustomValidator())
                .errorMessage(po.getErrorMessage())
                .fieldLabel(po.getFieldLabel())
                .fieldDescription(po.getFieldDescription())
                .placeholder(po.getPlaceholder())
                .enabled(po.getEnabled())
                .build();
    }

    private ModelValidationRulePO toRulePo(ModelValidationRuleEntity e) {
        ModelValidationRulePO po = new ModelValidationRulePO();
        po.setId(e.getId());
        po.setProviderName(e.getProviderName());
        po.setModelType(e.getModelType());
        po.setFieldName(e.getFieldName());
        po.setFieldType(e.getFieldType());
        po.setRequired(e.getRequired());
        po.setDefaultValue(e.getDefaultValue());
        po.setMinValue(e.getMinValue());
        po.setMaxValue(e.getMaxValue());
        po.setMinLength(e.getMinLength());
        po.setMaxLength(e.getMaxLength());
        po.setPattern(e.getPattern());
        po.setEnumValues(e.getEnumValues());
        po.setCustomValidator(e.getCustomValidator());
        po.setErrorMessage(e.getErrorMessage());
        po.setFieldLabel(e.getFieldLabel());
        po.setFieldDescription(e.getFieldDescription());
        po.setPlaceholder(e.getPlaceholder());
        po.setEnabled(e.getEnabled());
        return po;
    }

    @Override
    public BaseModelEntity queryModelById(ChatRequestEntity chatRequestEntity) {
        ChatRequest chatRequest=new ChatRequest();
        chatRequest.setModelId(chatRequestEntity.getModelId());
        chatRequest.setPrompt(chatRequestEntity.getPrompt());
        ModelConfig modelConfig =iModelDao.queryModelById(chatRequest);

        if("chat".equalsIgnoreCase(modelConfig.getType())){
            return ChatModelEntity.builder()
                    .providerName(modelConfig.getProviderName())
                    .modelId(modelConfig.getModelId())
                    .modelName(modelConfig.getModelName())
                    .url(modelConfig.getUrl())
                    .key(modelConfig.getKey())
                    .type(modelConfig.getType())
                    .temperature(modelConfig.getTemperature())
                    .topP(modelConfig.getTopP())
                    .maxTokens(modelConfig.getMaxTokens())
                    .stop(modelConfig.getStop() != null ? modelConfig.getStop().split(",") : null)
                    .frequencyPenalty(modelConfig.getFrequencyPenalty())
                    .presencePenalty(modelConfig.getPresencePenalty())
                    .systemPrompt(modelConfig.getSystemPrompt())
                    .maxContextLength(modelConfig.getMaxContextLength())
                    .supportStream(modelConfig.getSupportStream())
                    .supportFunctionCall(modelConfig.getSupportFunctionCall())
                    // 转换JSON字符串为Map
                    .dynamicProperties(JsonUtils.jsonToMap(modelConfig.getDynamicProperties()))
                    .version(modelConfig.getVersion())  // 添加版本号
                    .build();
        }else{
            return EmbeddingModelEntity.builder()
                    .modelId(modelConfig.getModelId())
                    .providerName(modelConfig.getProviderName())
                    .modelName(modelConfig.getModelName())
                    .url(modelConfig.getUrl())
                    .key(modelConfig.getKey())
                    .type(modelConfig.getType())
                    .embeddingFormat(modelConfig.getEmbeddingFormat())
                    .numPredict(modelConfig.getNumPredict())
                    .dimensions(modelConfig.getDimensions())
                    .maxInputLength(modelConfig.getMaxInputLength())
                    .supportBatch(modelConfig.getSupportBatch())
                    .maxBatchSize(modelConfig.getMaxBatchSize())
                    .normalize(modelConfig.getNormalize())
                    .similarityMetric(modelConfig.getSimilarityMetric())
                    // 转换JSON字符串为Map
                    .dynamicProperties(JsonUtils.jsonToMap(modelConfig.getDynamicProperties()))
                    .version(modelConfig.getVersion())  // 添加版本号
                    .build();
        }
    }

    @Override
    public void updateModelRecord(BaseModelEntity baseModelEntity) {
        int affectedRows;
        if ("chat".equalsIgnoreCase(baseModelEntity.getType())){
            ChatModelEntity chatModelEntity=(ChatModelEntity) baseModelEntity;
            ModelConfig modelConfig =new ModelConfig();
            modelConfig.setProviderName(chatModelEntity.getProviderName());
            modelConfig.setModelId(chatModelEntity.getModelId());
            modelConfig.setModelName(chatModelEntity.getModelName());
            modelConfig.setUrl(chatModelEntity.getUrl());
            modelConfig.setKey(chatModelEntity.getKey());
            modelConfig.setType(chatModelEntity.getType());
            modelConfig.setTemperature(chatModelEntity.getTemperature());
            modelConfig.setTopP(chatModelEntity.getTopP());
            modelConfig.setMaxTokens(chatModelEntity.getMaxTokens());
            modelConfig.setStop(Arrays.toString(chatModelEntity.getStop()));
            modelConfig.setFrequencyPenalty(chatModelEntity.getFrequencyPenalty());
            modelConfig.setPresencePenalty(chatModelEntity.getPresencePenalty());
            modelConfig.setSystemPrompt(chatModelEntity.getSystemPrompt());
            modelConfig.setMaxContextLength(chatModelEntity.getMaxContextLength());
            modelConfig.setSupportStream(chatModelEntity.getSupportStream());
            modelConfig.setSupportFunctionCall(chatModelEntity.getSupportFunctionCall());
            // 转换动态属性为JSON字符串
            modelConfig.setDynamicProperties(JsonUtils.mapToJson(chatModelEntity.getDynamicProperties()));
            modelConfig.setVersion(chatModelEntity.getVersion());
            affectedRows=iModelDao.updateModelRecord(modelConfig);
        }else {
            EmbeddingModelEntity embeddingModelEntity=(EmbeddingModelEntity) baseModelEntity;
            ModelConfig modelConfig =new ModelConfig();
            modelConfig.setProviderName(embeddingModelEntity.getProviderName());
            modelConfig.setModelId(embeddingModelEntity.getModelId());
            modelConfig.setModelName(embeddingModelEntity.getModelName());
            modelConfig.setUrl(embeddingModelEntity.getUrl());
            modelConfig.setKey(embeddingModelEntity.getKey());
            modelConfig.setType(embeddingModelEntity.getType());
            modelConfig.setEmbeddingFormat(embeddingModelEntity.getEmbeddingFormat());
            modelConfig.setNumPredict(embeddingModelEntity.getNumPredict());
            modelConfig.setDimensions(embeddingModelEntity.getDimensions());
            modelConfig.setMaxInputLength(embeddingModelEntity.getMaxInputLength());
            modelConfig.setSupportBatch(embeddingModelEntity.getSupportBatch());
            modelConfig.setMaxBatchSize(embeddingModelEntity.getMaxBatchSize());
            modelConfig.setNormalize(embeddingModelEntity.getNormalize());
            modelConfig.setSimilarityMetric(embeddingModelEntity.getSimilarityMetric());
            // 转换动态属性为JSON字符串
            modelConfig.setDynamicProperties(JsonUtils.mapToJson(embeddingModelEntity.getDynamicProperties()));
            modelConfig.setVersion(embeddingModelEntity.getVersion());
            affectedRows=iModelDao.updateModelRecord(modelConfig);
        }
        // 如果影响行数为0，抛出乐观锁异常
        if (affectedRows == 0) {
            throw new OptimisticLockException("模型配置更新失败，数据已被其他用户修改");
        }
    }

    @Override
    public void deleteModelRecord(String modelId) {
        iModelDao.deleteModelRecord(modelId);
    }

    @Override
    public BaseModelEntity queryModelById(String modelId) {
        ModelConfig modelConfig = iModelDao.queryModelById(modelId);
        if (modelConfig == null) {
            return null;
        }
        
        if("chat".equalsIgnoreCase(modelConfig.getType())){
            return ChatModelEntity.builder()
                    .modelId(modelConfig.getModelId())
                    .providerName(modelConfig.getProviderName())
                    .modelName(modelConfig.getModelName())
                    .url(modelConfig.getUrl())
                    .key(modelConfig.getKey())
                    .type(modelConfig.getType())
                    .temperature(modelConfig.getTemperature())
                    .topP(modelConfig.getTopP())
                    .maxTokens(modelConfig.getMaxTokens())
                    .stop(modelConfig.getStop() != null ? modelConfig.getStop().split(",") : null)
                    .frequencyPenalty(modelConfig.getFrequencyPenalty())
                    .presencePenalty(modelConfig.getPresencePenalty())
                    .systemPrompt(modelConfig.getSystemPrompt())
                    .maxContextLength(modelConfig.getMaxContextLength())
                    .supportStream(modelConfig.getSupportStream())
                    .supportFunctionCall(modelConfig.getSupportFunctionCall())
                    // 转换JSON字符串为Map
                    .dynamicProperties(JsonUtils.jsonToMap(modelConfig.getDynamicProperties()))
                    .version(modelConfig.getVersion())  // 添加版本号
                    .build();
        }else{
            return EmbeddingModelEntity.builder()
                    .modelId(modelConfig.getModelId())
                    .providerName(modelConfig.getProviderName())
                    .modelName(modelConfig.getModelName())
                    .url(modelConfig.getUrl())
                    .key(modelConfig.getKey())
                    .type(modelConfig.getType())
                    .embeddingFormat(modelConfig.getEmbeddingFormat())
                    .numPredict(modelConfig.getNumPredict())
                    .dimensions(modelConfig.getDimensions())
                    .maxInputLength(modelConfig.getMaxInputLength())
                    .supportBatch(modelConfig.getSupportBatch())
                    .maxBatchSize(modelConfig.getMaxBatchSize())
                    .normalize(modelConfig.getNormalize())
                    .similarityMetric(modelConfig.getSimilarityMetric())
                    // 转换JSON字符串为Map
                    .dynamicProperties(JsonUtils.jsonToMap(modelConfig.getDynamicProperties()))
                    .version(modelConfig.getVersion())  // 添加版本号
                    .build();
        }
    }
}

// 按你的规范：规则相关仓储作为同文件的另一个 @Repository 已移除，改为单文件仅此类，规则操作通过专用 DAO XML 暴露给服务层或新增独立服务类使用

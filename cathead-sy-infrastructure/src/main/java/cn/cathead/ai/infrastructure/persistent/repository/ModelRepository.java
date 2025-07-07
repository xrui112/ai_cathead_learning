package cn.cathead.ai.infrastructure.persistent.repository;

import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatRequestEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.infrastructure.persistent.dao.IModelDao;
import cn.cathead.ai.infrastructure.persistent.po.ModelConfig;
import cn.cathead.ai.infrastructure.persistent.po.ChatRequest;
import com.fasterxml.jackson.databind.ser.Serializers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.Arrays;

@Repository
@Slf4j
public class ModelRepository implements IModelRepository {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IModelDao iModelDao;
    @Override
    public void saveModelRecord(BaseModelEntity baseModelEntity) {
        //redis 不存了 存数据库就行
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
            iModelDao.saveModelRecord(modelConfig);

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
            iModelDao.saveModelRecord(modelConfig);

        }



    }

    @Override
    public BaseModelEntity queryModelById(ChatRequestEntity chatRequestEntity) {
        ChatRequest chatRequest=new ChatRequest();
        chatRequest.setModelId(chatRequestEntity.getModelId());
        chatRequest.setPrompt(chatRequestEntity.getPrompt());
        ModelConfig modelConfig =iModelDao.queryModelById(chatRequest);

        //todo  区分不同模型
        if("chat".equalsIgnoreCase(modelConfig.getType())){
            return ChatModelEntity.builder()
                    .providerName(modelConfig.getProviderName())
                    .modelId(modelConfig.getModelId())
                    .modelName(modelConfig.getModelName())
                    .url(modelConfig.getUrl())
                    .key(modelConfig.getKey())
                    .build();
        }else{
            return EmbeddingModelEntity.builder()
                    .modelId(modelConfig.getModelId())
                    .providerName(modelConfig.getProviderName())
                    .modelName(modelConfig.getModelName())
                    .url(modelConfig.getUrl())
                    .key(modelConfig.getKey())
                    .type(modelConfig.getType())                      // 如果 EmbeddingModelEntity 有 type 字段
                    .embeddingFormat(modelConfig.getEmbeddingFormat()) // 如果 EmbeddingModelEntity 有 embeddingFormat 字段
                    .numPredict(modelConfig.getNumPredict())           // 如果 EmbeddingModelEntity 有 numPredict 字段
                    .build();
        }
    }

    @Override
    public void updateModelRecord(BaseModelEntity baseModelEntity) {
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
            iModelDao.updateModelRecord(modelConfig);
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
            iModelDao.updateModelRecord(modelConfig);
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
                    .build();
        }
    }
}

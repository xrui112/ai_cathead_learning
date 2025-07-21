package cn.cathead.ai.infrastructure.persistent.repository;

import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatRequestEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.infrastructure.persistent.dao.IModelDao;
import cn.cathead.ai.infrastructure.persistent.po.ModelConfig;
import cn.cathead.ai.infrastructure.persistent.po.ChatRequest;
import cn.cathead.ai.types.exception.OptimisticLockException;
import com.fasterxml.jackson.databind.ser.Serializers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Arrays;

@Repository
@Slf4j
public class ModelRepository implements IModelRepository {


    @Resource
    private IModelDao iModelDao;


    @Override
    public long saveModelRecord(BaseModelEntity baseModelEntity) {
        //redis 不存了 存数据库就行
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
            version=iModelDao.saveModelRecord(modelConfig);

        }
        return version;
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
                    .version(modelConfig.getVersion())  // 添加版本号
                    .build();
        }
    }
}

package cn.cathead.ai.domain.model.service.registry.modelcreation;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.registry.modelcache.IModelCacheManager;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 模型创建服务实现类
 * 专门负责模型的创建逻辑，从ModelService中分离出来避免循环依赖
 */
@Service
@Slf4j
public class ModelCreationService implements IModelCreationService {

    @Resource
    private IModelRepository iModelRepository;

    @Resource
    private IModelCacheManager modelCacheManager;

    @Override
    public String createChatModel(ChatModelDTO chatModelDTO) {
        ChatModelEntity chatModelEntity = ChatModelEntity.builder()
                .modelId(UUID.randomUUID().toString())
                .providerName(chatModelDTO.getProviderName())
                .modelName(chatModelDTO.getModelName())
                .url(chatModelDTO.getUrl())
                .key(chatModelDTO.getKey())
                .type(chatModelDTO.getType())
                .temperature(chatModelDTO.getTemperature())
                .topP(chatModelDTO.getTopP())
                .maxTokens(chatModelDTO.getMaxTokens())
                .presencePenalty(chatModelDTO.getPresencePenalty())
                .frequencyPenalty(chatModelDTO.getFrequencyPenalty())
                .stop(chatModelDTO.getStop())
                .build();

        // 1. 创建模型实例
        ChatModel chatModel = modelCacheManager.createChatModelInstance(chatModelEntity);

        if (chatModel == null) {
            log.error("创建Chat模型实例失败: {}", chatModelEntity.getModelName());
            return null;
        }

        // 2. 存储到数据库（获得version） 初始version是0
        long version = iModelRepository.saveModelRecord(chatModelEntity);
        // 设置版本号到实体对象中
        chatModelEntity.setVersion(version);
        log.info("Chat模型存储到数据库成功，模型ID: {}, 版本: {}", 
                chatModelEntity.getModelId(), chatModelEntity.getVersion());

        // 3. 存入缓存
        modelCacheManager.saveChatModelToCache(chatModel, chatModelEntity);
        log.info("Chat模型创建成功: {}", chatModelEntity.getModelId());
        return chatModelEntity.getModelId();
    }

    @Override
    public String createEmbeddingModel(EmbeddingModelDTO embeddingModelDTO) {
        EmbeddingModelEntity embeddingModelEntity = EmbeddingModelEntity.builder()
                .modelId(UUID.randomUUID().toString())
                .providerName(embeddingModelDTO.getProviderName())
                .modelName(embeddingModelDTO.getModelName())
                .url(embeddingModelDTO.getUrl())
                .key(embeddingModelDTO.getKey())
                .type(embeddingModelDTO.getType())
                .embeddingFormat(embeddingModelDTO.getEmbeddingFormat())
                .numPredict(embeddingModelDTO.getNumPredict())
                .dimensions(embeddingModelDTO.getDimensions())
                .build();

        // 1. 创建模型实例
        EmbeddingModel embeddingModel = modelCacheManager.createEmbeddingModelInstance(embeddingModelEntity);

        if (embeddingModel == null) {
            log.error("创建Embedding模型实例失败: {}", embeddingModelEntity.getModelName());
            return null;
        }

        // 2. 存储到数据库（获得version）
        long version = iModelRepository.saveModelRecord(embeddingModelEntity);
        // 设置版本号到实体对象中
        embeddingModelEntity.setVersion(version);
        log.info("Embedding模型存储到数据库成功，模型ID: {}, 版本: {}", 
                embeddingModelEntity.getModelId(), embeddingModelEntity.getVersion());

        // 3. 存入缓存
        modelCacheManager.saveEmbeddingModelToCache(embeddingModel, embeddingModelEntity);
        log.info("Embedding模型创建成功: {}", embeddingModelEntity.getModelId());
        return embeddingModelEntity.getModelId();
    }
} 
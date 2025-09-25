package cn.cathead.ai.domain.model.service;

import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.runtime.chat.IChatService;
import cn.cathead.ai.domain.model.service.runtime.embedding.IEmbeddingService;
import cn.cathead.ai.domain.model.service.registry.modelcache.IModelCacheManager;
import cn.cathead.ai.domain.model.service.registry.IModelProviderService;
import cn.cathead.ai.domain.model.service.registry.modelcreation.IModelCreationService;
import cn.cathead.ai.domain.model.service.registry.update.impl.ChatModelUpdateService;
import cn.cathead.ai.domain.model.service.registry.update.impl.EmbeddingModelUpdateService;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;


@Service
@Slf4j
public class ModelService implements IModelService {

    @Resource
    private IModelRepository modelRepository;

    // 使用接口来管理模型Bean
    @Resource
    private IModelCacheManager modelBeanManager;

    // 模型提供者服务
    @Resource
    private IModelProviderService modelProviderService;


    // 模型创建服务
    @Resource
    private IModelCreationService modelCreationService;

    // 模型更新服务
    @Resource
    private ChatModelUpdateService chatModelUpdateService;

    @Resource
    private EmbeddingModelUpdateService embeddingModelUpdateService;

    // Chat子领域服务
    @Resource
    private IChatService chatService;

    // Embedding子领域服务
    @Resource
    private IEmbeddingService embeddingService;

    /**
     * 流式聊天接口
     */
    @Override
    public Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto) {
        return chatService.chatWithStream(chatRequestDto);
    }

    /**
     * 普通聊天接口
     */
    @Override
    public ChatResponse chatWith(ChatRequestDTO chatRequestDto) {
        return chatService.chatWith(chatRequestDto);
    }

    /**
     * 文本向量化接口
     */
    @Override
    public EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto) {
        return embeddingService.embedText(embeddingRequestDto);
    }

    @Override
    public void deleteModel(String modelId) {
        log.info("开始删除模型，模型ID: {}", modelId);

        // 1. 从ModelBeanManager中移除
        modelBeanManager.removeChatModelBean(modelId);
        modelBeanManager.removeEmbeddingModelBean(modelId);

        // 2. 删除数据库记录
        modelRepository.deleteModelRecord(modelId);

        log.info("模型删除成功，模型ID: {}", modelId);
    }

    @Override
    public BaseModelEntity getModelById(String modelId) {
        return modelRepository.queryModelById(modelId);
    }

    public EmbeddingModel getLatestEmbeddingModel(String modelId) {
        return modelProviderService.getEmbeddingModel(modelId);
    }

    public ChatModel getLatestChatModel(String modelId) {
        return modelProviderService.getChatModel(modelId);
    }

    public String getModelVersionStatus(String modelId) {
        BaseModelEntity dbEntity = modelRepository.queryModelById(modelId);
        if (dbEntity == null) {
            return String.format("模型[%s]不存在", modelId);
        }

        Long cachedVersion = modelBeanManager.getCachedModelVersion(modelId);
        Long dbVersion = dbEntity.getVersion();

        if (cachedVersion == null) {
            return String.format("模型[%s]：缓存中不存在，数据库版本: %d", modelId, dbVersion);
        }

        if (cachedVersion.equals(dbVersion)) {
            return String.format("模型[%s]：缓存版本与数据库版本一致，版本: %d", modelId, dbVersion);
        } else {
            return String.format("模型[%s]：缓存版本过期，缓存版本: %d，数据库版本: %d",
                    modelId, cachedVersion, dbVersion);
        }
    }

    @Override
    public void refreshModelCache(String modelId) {
        modelBeanManager.refreshModelCache(modelId);
    }

    // 统一入口：委托给 ModelCreationService
    @Override
    public String createChatModel(ChatModelDTO chatModelDTO) {
        return modelCreationService.createChatModel(chatModelDTO);
    }

    @Override
    public String createEmbeddingModel(EmbeddingModelDTO embeddingModelDTO) {
        return modelCreationService.createEmbeddingModel(embeddingModelDTO);
    }
}

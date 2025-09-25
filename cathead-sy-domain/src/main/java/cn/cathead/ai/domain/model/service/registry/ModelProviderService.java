package cn.cathead.ai.domain.model.service.registry;

import cn.cathead.ai.domain.model.service.registry.modelcache.IModelCacheManager;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * 模型提供者服务实现
 * 提供统一的模型获取方法，封装从缓存获取模型的逻辑
 */
@Service
@Slf4j
public class ModelProviderService implements IModelProviderService {

    private final IModelCacheManager modelCacheManager;

    public ModelProviderService(IModelCacheManager modelCacheManager) {
        this.modelCacheManager = modelCacheManager;
    }

    @Override
    public ChatModel getChatModel(String modelId) {
        log.debug("获取Chat模型，模型ID: {}", modelId);
        return modelCacheManager.ensureLatestChatModel(modelId);
    }

    @Override
    public EmbeddingModel getEmbeddingModel(String modelId) {
        log.debug("获取Embedding模型，模型ID: {}", modelId);
        return modelCacheManager.ensureLatestEmbeddingModel(modelId);
    }

    @Override
    public ChatModel getAndValidateChatModel(String modelId) {
        log.info("获取并验证Chat模型，模型ID: {}", modelId);
        ChatModel chatModel = getChatModel(modelId);
        if (chatModel == null) {
            String errorMsg = String.format("Chat模型[%s]不存在或获取失败", modelId);
            log.error(errorMsg);
            throw new AppException(ResponseCode.FAILED_CHAT.getCode(), errorMsg);
        }
        return chatModel;
    }

    @Override
    public EmbeddingModel getAndValidateEmbeddingModel(String modelId) {
        log.info("获取并验证Embedding模型，模型ID: {}", modelId);
        EmbeddingModel embeddingModel = getEmbeddingModel(modelId);
        if (embeddingModel == null) {
            String errorMsg = String.format("Embedding模型[%s]不存在或获取失败", modelId);
            log.error(errorMsg);
            throw new AppException(ResponseCode.FAILED_EMBEDDING.getCode(), errorMsg);
        }
        return embeddingModel;
    }
}

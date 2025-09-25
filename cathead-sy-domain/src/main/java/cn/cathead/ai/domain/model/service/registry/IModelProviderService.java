package cn.cathead.ai.domain.model.service.registry;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * 模型提供者服务接口
 * 提供统一的模型获取方法，封装从缓存获取模型的逻辑
 */
public interface IModelProviderService {

    /**
     * 获取Chat模型
     * @param modelId 模型ID
     * @return Chat模型实例
     */
    ChatModel getChatModel(String modelId);

    /**
     * 获取Embedding模型
     * @param modelId 模型ID
     * @return Embedding模型实例
     */
    EmbeddingModel getEmbeddingModel(String modelId);

    /**
     * 获取并验证Chat模型
     * @param modelId 模型ID
     * @return Chat模型实例
     * @throws cn.cathead.ai.types.exception.AppException 当模型不存在或获取失败时
     */
    ChatModel getAndValidateChatModel(String modelId);

    /**
     * 获取并验证Embedding模型
     * @param modelId 模型ID
     * @return Embedding模型实例
     * @throws cn.cathead.ai.types.exception.AppException 当模型不存在或获取失败时
     */
    EmbeddingModel getAndValidateEmbeddingModel(String modelId);
}

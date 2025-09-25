package cn.cathead.ai.domain.model.service.registry.modelcreation;

import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;

/**
 * 模型创建服务接口
 * 专门负责模型的创建逻辑，避免循环依赖
 */
public interface IModelCreationService {
    
    /**
     * 创建Chat模型
     * @param chatModelDTO Chat模型配置
     */
    String createChatModel(ChatModelDTO chatModelDTO);
    
    /**
     * 创建Embedding模型
     * @param embeddingModelDTO Embedding模型配置
     */
    String createEmbeddingModel(EmbeddingModelDTO embeddingModelDTO);
} 
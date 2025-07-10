package cn.cathead.ai.domain.model.service;

import cn.cathead.ai.api.dto.BaseModelDTO;

import cn.cathead.ai.api.dto.ChatModelDTO;
import cn.cathead.ai.api.dto.ChatRequestDto;
import cn.cathead.ai.api.dto.EmbeddingModelDTO;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatRequestEntity;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import reactor.core.publisher.Flux;

public interface IModelService {

    public void creatModel(ChatModelDTO chatModelDTO);

    public void creatModel(EmbeddingModelDTO embeddingModelDTO);

    public Flux<ChatResponse> chatWith(ChatRequestDto chatRequestDto);

    public void updateChatModelConfig(String modelId, ChatModelDTO chatModelDTO);

    public void updateEmbeddingModelConfig(String modelId, EmbeddingModelDTO embeddingModelDTO);

    public void deleteModel(String modelId);

    public BaseModelEntity getModelById(String modelId);

    public void refreshModelCache(String modelId);

    /**
     * 获取最新版本的Embedding模型
     * @param modelId 模型ID
     * @return 最新版本的Embedding模型，如果不存在返回null
     */
    public EmbeddingModel getLatestEmbeddingModel(String modelId);

    /**
     * 获取最新版本的Chat模型
     * @param modelId 模型ID
     * @return 最新版本的Chat模型，如果不存在返回null
     */
    public ChatModel getLatestChatModel(String modelId);

    /**
     * 检查模型版本状态
     * @param modelId 模型ID
     * @return 版本状态信息
     */
    public String getModelVersionStatus(String modelId);

}

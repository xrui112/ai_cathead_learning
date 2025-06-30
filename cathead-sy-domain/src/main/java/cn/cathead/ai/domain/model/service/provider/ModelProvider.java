package cn.cathead.ai.domain.model.service.provider;

import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

public interface ModelProvider {
    ChatModel createChat(ChatModelEntity chatModelEntity);

    EmbeddingModel createEmbedding(EmbeddingModelEntity embeddingModelEntity);

}

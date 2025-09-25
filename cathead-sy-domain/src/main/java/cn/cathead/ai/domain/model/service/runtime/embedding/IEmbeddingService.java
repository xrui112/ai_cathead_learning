package cn.cathead.ai.domain.model.service.runtime.embedding;

import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Embedding子领域服务接口
 * 负责处理文本向量化相关的业务逻辑
 */
public interface IEmbeddingService {

    EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto);

    EmbeddingResponse generateEmbedding(EmbeddingModel embeddingModel, EmbeddingRequestDTO request);
}

package cn.cathead.ai.domain.client.repository;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;

import java.util.List;

/**
 * 长期记忆仓储接口（结构化摘要 + 向量检索）
 */
public interface ILongTermMemoryRepository {

    void save(MemoryChunk chunk);

    void saveAll(List<MemoryChunk> chunks);

    /**
     * 使用查询文本进行相似检索（仓储内部完成向量化）
     */
    List<MemoryChunk> semanticSearchByText(String knowledgeId, String agentId, String queryText, String embeddingModelId, int topK);

    void deleteById(String id);

    /**
     * 保存长期记忆并进行向量化（仓储内部完成向量化）
     */
    void saveAndEmbed(MemoryChunk chunk, String embeddingModelId);

    void saveAllAndEmbed(List<MemoryChunk> chunks, String embeddingModelId);
}



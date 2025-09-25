package cn.cathead.ai.domain.client.service.advisor.memory.manager.longterm;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import org.springframework.ai.document.Document;

import java.util.List;

public interface ILongTermMemoryService {

    void saveText(String sessionId, String knowledgeId, String agentId, String title, String text,
                  List<String> tags, Double importance, String embeddingModelId,
                  int chunkSize, int overlap);

    void saveDocuments(String sessionId, String knowledgeId, String agentId, String title,
                       List<Document> documents, List<String> tags, Double importance,
                       String embeddingModelId, int chunkSize, int overlap);

    List<MemoryChunk> searchByText(String knowledgeId, String agentId, String query, String embeddingModelId, int topK);

    
}



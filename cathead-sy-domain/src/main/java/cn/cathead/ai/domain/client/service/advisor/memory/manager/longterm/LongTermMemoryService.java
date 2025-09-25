    package cn.cathead.ai.domain.client.service.advisor.memory.manager.longterm;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import cn.cathead.ai.domain.client.repository.ILongTermMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.config.MemoryProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LongTermMemoryService implements ILongTermMemoryService {

    private final ILongTermMemoryRepository longTermRepo;
    private final MemoryProperties props;

    @Override
    public void saveText(String sessionId, String knowledgeId, String agentId, String title, String text,
                         List<String> tags, Double importance, String embeddingModelId,
                         int chunkSize, int overlap) {



        String fullText = buildFullText(title, text);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", title);
        metadata.put("sessionId", sessionId);
        metadata.put("knowledgeId", knowledgeId);
        metadata.put("agentId", agentId);
        if (tags != null) metadata.put("tags", String.join(",", tags));
        if (importance != null) metadata.put("importance", importance);

        List<Document> docs = Collections.singletonList(new Document(fullText, metadata));
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splits = splitter.split(docs);
        persistDocumentsAsChunks(splits, sessionId, knowledgeId, agentId, title, tags, importance, embeddingModelId);
    }

    @Override
    public void saveDocuments(String sessionId, String knowledgeId, String agentId, String title,
                              List<Document> documents, List<String> tags, Double importance,
                              String embeddingModelId, int chunkSize, int overlap) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splits = splitter.split(documents);
        persistDocumentsAsChunks(splits, sessionId, knowledgeId, agentId, title, tags, importance, embeddingModelId);
    }

    public List<MemoryChunk> searchByText(String knowledgeId, String agentId, String query, String embeddingModelId, int topK) {
        return longTermRepo.semanticSearchByText(knowledgeId, agentId, query, embeddingModelId, topK);
    }

    private static String buildFullText(String title, String summary) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) sb.append(title).append('\n');
        if (summary != null) sb.append(summary);
        return sb.toString();
    }

    private void persistDocumentsAsChunks(List<Document> splits, String sessionId, String knowledgeId, String agentId,
                                          String title, List<String> tags, Double importance, String embeddingModelId) {
        if (splits == null || splits.isEmpty()) {
            splits = Collections.singletonList(new Document("", Collections.emptyMap()));
        }

        List<MemoryChunk> chunks = new ArrayList<>(splits.size());

        for (Document d : splits) {
            String id = java.util.UUID.randomUUID().toString();
            MemoryChunk chunk = MemoryChunk.builder()
                    .id(id)
                    .sessionId(sessionId)
                    .knowledgeId(knowledgeId)
                    .agentId(agentId)
                    .title(title)
                    .summary(d.getText())
                    .tags(tags)
                    .importanceScore(importance == null ? null : cn.cathead.ai.domain.client.model.valobj.ImportanceScore.of(importance, "manual"))
                    .createdAt(Instant.now())
                    .lastAccessAt(Instant.now())
                    .build();
            chunks.add(chunk);

        }

        longTermRepo.saveAllAndEmbed(chunks, embeddingModelId);
    }
}



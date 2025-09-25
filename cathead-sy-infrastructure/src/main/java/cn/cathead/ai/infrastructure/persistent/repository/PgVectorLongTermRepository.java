package cn.cathead.ai.infrastructure.persistent.repository;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import cn.cathead.ai.domain.client.repository.ILongTermMemoryRepository;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MemoryContextHolder;
import cn.cathead.ai.domain.model.service.IModelService;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.config.MemoryProperties;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@Primary
@RequiredArgsConstructor
public class PgVectorLongTermRepository implements ILongTermMemoryRepository {

    private final JdbcTemplate jdbcTemplate;

    private final IModelService modelService;

    private final MemoryProperties props;

    @Override
    public void save(MemoryChunk chunk) {
        saveAll(List.of(chunk));
    }

    @Override
    public void saveAll(List<MemoryChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;
        String sql = "INSERT INTO long_term_memory (id, session_id, knowledge_id, agent_id, title, summary, tags, importance, created_at, last_access_at, embedding) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL) " +
                "ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, summary = EXCLUDED.summary, tags = EXCLUDED.tags, importance = EXCLUDED.importance, last_access_at = EXCLUDED.last_access_at";

        for (MemoryChunk c : chunks) {
            String tags = c.getTags() == null ? null : String.join(",", c.getTags());
            jdbcTemplate.update(sql,
                    c.getId(),
                    c.getSessionId(),
                    c.getKnowledgeId(),
                    c.getAgentId(),
                    c.getTitle(),
                    c.getSummary(),
                    tags,
                    c.getImportanceScore() == null ? 0.0 : c.getImportanceScore().getScore(),
                    toTimestamp(c.getCreatedAt()),
                    toTimestamp(c.getLastAccessAt())
            );
        }
    }

    @Override
    public List<MemoryChunk> semanticSearchByText(String knowledgeId, String agentId, String queryText, String embeddingModelId, int topK) {
        String modelId = (embeddingModelId == null || embeddingModelId.isBlank()) ? props.getLtm().getDefaultEmbeddingModelId() : embeddingModelId;
        if (modelId == null || modelId.isBlank()) {
            // 未配置 embedding 模型，直接返回空结果，避免打断主链路
            return List.of();
        }
        float[] queryEmbedding;
        try {
            queryEmbedding = toFloatArray(modelService.embedText(new EmbeddingRequestDTO(modelId, List.of(queryText))).getResults().get(0).getOutput());
        } catch (Exception e) {
            // 调用向量模型失败，返回空结果
            return List.of();
        }
        String vec = toPgVectorLiteral(queryEmbedding);
        String sql = "SELECT id, session_id, knowledge_id, agent_id, title, summary, tags, importance, created_at, last_access_at " +
                "FROM long_term_memory " +
                "WHERE (session_id IS NULL OR session_id = ?) AND (knowledge_id IS NULL OR knowledge_id = ?) AND (agent_id IS NULL OR agent_id = ?) " +
                "ORDER BY embedding <=> ?::vector LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, currentSessionId(), knowledgeId, agentId, vec, topK);
    }

    // 文本检索逻辑应位于中间服务层，这里仅保留向量检索

    // 保存并向量化应位于中间服务层，这里仅保留 save/saveAll 持久化和向量检索

    @Override
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM long_term_memory WHERE id = ?", id);
    }

    private void saveWithEmbeddingVector(MemoryChunk chunk, float[] embedding) {
        String sql = "INSERT INTO long_term_memory (id, session_id, knowledge_id, agent_id, title, summary, tags, importance, created_at, last_access_at, embedding) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector) " +
                "ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, summary = EXCLUDED.summary, tags = EXCLUDED.tags, importance = EXCLUDED.importance, last_access_at = EXCLUDED.last_access_at, embedding = EXCLUDED.embedding";

        String tags = chunk.getTags() == null ? null : String.join(",", chunk.getTags());
        jdbcTemplate.update(sql,
                chunk.getId(),
                chunk.getSessionId(),
                chunk.getKnowledgeId(),
                chunk.getAgentId(),
                chunk.getTitle(),
                chunk.getSummary(),
                tags,
                chunk.getImportanceScore() == null ? 0.0 : chunk.getImportanceScore().getScore(),
                toTimestamp(chunk.getCreatedAt()),
                toTimestamp(chunk.getLastAccessAt()),
                toPgVectorLiteral(embedding)
        );
    }

    @Override
    public void saveAndEmbed(MemoryChunk chunk, String embeddingModelId) {
        if (chunk == null) return;
        saveAllAndEmbed(List.of(chunk), embeddingModelId);
    }

    @Override
    public void saveAllAndEmbed(List<MemoryChunk> chunks, String embeddingModelId) {
        if (chunks == null || chunks.isEmpty()) return;
        String modelId = (embeddingModelId == null || embeddingModelId.isBlank()) ? props.getLtm().getDefaultEmbeddingModelId() : embeddingModelId;
        for (MemoryChunk c : chunks) {
            float[] vec = toFloatArray(modelService.embedText(new EmbeddingRequestDTO(modelId, List.of(c.getSummary()))).getResults().get(0).getOutput());
            saveWithEmbeddingVector(c, vec);
        }
    }

    // 便捷文本保存由上层服务实现，这里删除

    private static java.sql.Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : java.sql.Timestamp.from(instant);
    }

    private static final RowMapper<MemoryChunk> ROW_MAPPER = new RowMapper<>() {
        @Override
        public MemoryChunk mapRow(ResultSet rs, int rowNum) throws SQLException {
            String tagsStr = rs.getString("tags");
            List<String> tags = tagsStr == null ? new ArrayList<>() : List.of(tagsStr.split(","));
            return MemoryChunk.builder()
                    .id(rs.getString("id"))
                    .sessionId(rs.getString("session_id"))
                    .knowledgeId(rs.getString("knowledge_id"))
                    .agentId(rs.getString("agent_id"))
                    .title(rs.getString("title"))
                    .summary(rs.getString("summary"))
                    .tags(tags)
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .lastAccessAt(rs.getTimestamp("last_access_at") == null ? null : rs.getTimestamp("last_access_at").toInstant())
                    .importanceScore(null)
                    .build();
        }
    };

    private String currentSessionId() {
        return MemoryContextHolder.getSessionId();
    }

    // 向量生成与文本切分交由上层服务处理

    private static String toPgVectorLiteral(float[] vec) {
        if (vec == null || vec.length == 0) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static float[] toFloatArray(Object output) {
        if (output == null) return new float[0];
        if (output instanceof float[] f) {
            return f;
        }
        if (output instanceof double[] d) {
            float[] f = new float[d.length];
            for (int i = 0; i < d.length; i++) f[i] = (float) d[i];
            return f;
        }
        if (output instanceof List<?> list) {
            float[] f = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object v = list.get(i);
                if (v instanceof Number n) f[i] = n.floatValue();
                else f[i] = 0f;
            }
            return f;
        }
        throw new IllegalArgumentException("Unsupported embedding output type: " + output.getClass());
    }
}



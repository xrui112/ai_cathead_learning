package cn.cathead.ai.domain.client.model.entity;

import cn.cathead.ai.domain.client.model.valobj.ImportanceScore;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

/**
 * 结构化记忆片段（压缩后的摘要，长期持久化候选）
 */
@Getter
@ToString
@Builder
public class MemoryChunk {
    private final String id;
    private final String title;            // 摘要标题
    private final String summary;          // 结构化摘要正文
    private final List<String> tags;       // 标签
    private final ImportanceScore importanceScore;
    private final Instant createdAt;
    private final Instant lastAccessAt;
    private final String sessionId;
    private final String knowledgeId;
    private final String agentId;
}



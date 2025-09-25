package cn.cathead.ai.domain.client.model.valobj;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 命名空间键，用于多Agent/多项目的记忆隔离
 */
@Getter
@EqualsAndHashCode
@ToString
public class NamespaceKey {

    /** 组织/用户/项目等上层域，可空 */
    private final String knowledgeId;
    /** Agent 标识 */
    private final String agentId;
    /** 会话标识 */
    private final String sessionId;

    public NamespaceKey(String knowledgeId, String agentId, String sessionId) {
        this.knowledgeId = knowledgeId;
        this.agentId = agentId;
        this.sessionId = sessionId;
    }

    public static NamespaceKey of(String knowledgeId, String agentId, String sessionId) {
        return new NamespaceKey(knowledgeId, agentId, sessionId);
    }
}



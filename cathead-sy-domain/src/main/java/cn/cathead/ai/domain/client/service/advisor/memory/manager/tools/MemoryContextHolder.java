package cn.cathead.ai.domain.client.service.advisor.memory.manager.tools;

/**
 * 记忆上下文持有者：通过 ThreadLocal 贯穿 sessionId/agentId/knowledgeId
 */
public final class MemoryContextHolder {

    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> AGENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> KNOWLEDGE_ID = new ThreadLocal<>();

    private MemoryContextHolder() {}

    public static void set(String sessionId, String agentId, String knowledgeId) {
        SESSION_ID.set(sessionId);
        AGENT_ID.set(agentId);
        KNOWLEDGE_ID.set(knowledgeId);
    }

    public static String getSessionId() { return SESSION_ID.get(); }
    public static String getAgentId() { return AGENT_ID.get(); }
    public static String getKnowledgeId() { return KNOWLEDGE_ID.get(); }

    public static void clear() {
        SESSION_ID.remove();
        AGENT_ID.remove();
        KNOWLEDGE_ID.remove();
    }
}



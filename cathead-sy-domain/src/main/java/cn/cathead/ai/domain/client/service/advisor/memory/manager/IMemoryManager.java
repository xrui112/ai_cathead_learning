package cn.cathead.ai.domain.client.service.advisor.memory.manager;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import cn.cathead.ai.domain.client.model.valobj.NamespaceKey;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 记忆管理器接口
 */
public interface IMemoryManager {

    List<Message> getContext(String sessionId);

    void updateMemory(String sessionId, List<Message> newMessages);

    void clear(String sessionId);

    List<MemoryChunk> retrieveLongTerm(String knowledgeId, String agentId, String query, int topK);

    NamespaceKey resolveNamespace(String sessionId);
}



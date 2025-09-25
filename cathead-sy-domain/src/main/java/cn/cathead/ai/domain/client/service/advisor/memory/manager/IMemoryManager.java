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

    /**
     * 显式写入短期记忆（与 updateMemory 语义一致，但面向业务主动调用场景）。
     */
    void saveShortTermMessages(String sessionId, List<Message> messages);

    /**
     * 便捷方法：以用户消息的形式写入短期记忆。
     */
    void saveShortTermTextAsUser(String sessionId, String text);

    /**
     * 便捷方法：以助手消息的形式写入短期记忆。
     */
    void saveShortTermTextAsAssistant(String sessionId, String text);

    void clear(String sessionId);

    List<MemoryChunk> retrieveLongTerm(String knowledgeId, String agentId, String query, int topK);

    NamespaceKey resolveNamespace(String sessionId);

    /**
     * 显式写入长期记忆（需外部调用触发）。
     */
    MemoryChunk saveLongTermText(String sessionId, String knowledgeId, String agentId,
                                 String title, String text, List<String> tags, Double importance);
}



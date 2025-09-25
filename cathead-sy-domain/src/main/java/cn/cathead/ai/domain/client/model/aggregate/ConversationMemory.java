package cn.cathead.ai.domain.client.model.aggregate;

import cn.cathead.ai.domain.client.model.entity.MemoryMessage;
import cn.cathead.ai.domain.client.model.valobj.NamespaceKey;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话短期记忆聚合根
 */
@Getter
@ToString
public class ConversationMemory {
    private final NamespaceKey namespaceKey;
    private final List<MemoryMessage> rollingBuffer;
    private double totalEstimatedTokens;

    public ConversationMemory(NamespaceKey namespaceKey) {
        this.namespaceKey = namespaceKey;
        this.rollingBuffer = new ArrayList<>();
        this.totalEstimatedTokens = 0.0;
    }

    public List<MemoryMessage> snapshot() {
        return Collections.unmodifiableList(rollingBuffer);
    }

    public void append(MemoryMessage message) {
        rollingBuffer.add(message);
        totalEstimatedTokens += Math.max(0.0, message.getTokenCost());
    }

    public double getTotalEstimatedTokens() {
        return totalEstimatedTokens;
    }
}



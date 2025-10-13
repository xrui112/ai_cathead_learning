package cn.cathead.ai.domain.client.service.advisor.memory.manager;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import cn.cathead.ai.domain.client.model.entity.MemoryMessage;
import cn.cathead.ai.domain.client.model.valobj.NamespaceKey;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.config.MemoryProperties;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.instant.IShortTermMemoryService;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.instant.compress.IMemoryCompressor;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.longterm.LongTermMemoryService;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MemoryContextHolder;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MessageUtils;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.ShortTermPolicy;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.ShortTermPolicyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMemoryManager implements IMemoryManager {

    private final IShortTermMemoryService shortTermRepo;
    private final IMemoryCompressor memoryCompressor;
    private final ShortTermPolicyProvider policyProvider;
    private final LongTermMemoryService longTermMemoryService;
    private final MemoryProperties props;


    @Override
    public List<Message> getContext(String sessionId) {
        NamespaceKey ns = resolveNamespace(sessionId);
        return shortTermRepo.get(ns).stream().map(MemoryMessage::getPayload).toList();
    }

    @Override
    public void updateMemory(String sessionId, List<Message> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) return;
        NamespaceKey ns = resolveNamespace(sessionId);
        List<MemoryMessage> wrapped = wrap(newMessages);
        shortTermRepo.append(ns, wrapped);

        ShortTermPolicy policy = policyProvider.getPolicy(ns.getKnowledgeId(), ns.getAgentId());
        double tokens = shortTermRepo.totalEstimatedTokens(ns);
        int count = shortTermRepo.size(ns);
        if (policy.shouldCompress(tokens, count)) {
            // 仅异步压缩，读取方将继续读取旧版，压缩完成后原子替换
            shortTermRepo.compressNamespaceAsync(ns, policy, memoryCompressor);
        }
    }

    @Override
    public void saveShortTermMessages(String sessionId, List<Message> messages) {
        updateMemory(sessionId, messages);
    }

    @Override
    public void saveShortTermTextAsUser(String sessionId, String text) {
        if (text == null || text.isBlank()) return;
        updateMemory(sessionId, List.of(org.springframework.ai.chat.messages.UserMessage.builder().text(text).build()));
    }

    @Override
    public void saveShortTermTextAsAssistant(String sessionId, String text) {
        if (text == null || text.isBlank()) return;
        updateMemory(sessionId, List.of(new org.springframework.ai.chat.messages.AssistantMessage(text)));
    }

    @Override
    public void clear(String sessionId) {
        shortTermRepo.clear(resolveNamespace(sessionId));
    }

    @Override
    public List<MemoryChunk> retrieveLongTerm(String knowledgeId, String agentId, String query, int topK) {
        if (knowledgeId == null || knowledgeId.isBlank()) {
            log.warn("retrieveLongTerm: knowledgeId 为空，跳过长期记忆召回");
            return List.of();
        }
        // 使用统一配置的默认 embedding 模型ID
        String modelId = props.getLtm().getDefaultEmbeddingModelId();
        return longTermMemoryService.searchByText(knowledgeId, agentId, query, modelId, topK);
    }

    @Override
    public NamespaceKey resolveNamespace(String sessionId) {
        // 从上下文持有者中读取 agentId/knowledgeId，实现 STM 以 (sessionId, agentId) 隔离
        String agentId = MemoryContextHolder.getAgentId();
        String knowledgeId = MemoryContextHolder.getKnowledgeId();
        return NamespaceKey.of(knowledgeId, agentId, sessionId);
    }

    private List<MemoryMessage> wrap(List<Message> messages) {
        List<MemoryMessage> list = new ArrayList<>(messages.size());
        for (Message m : messages) {
            list.add(MemoryMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .payload(m)
                    .createdAt(Instant.now())
                    .tokenCost(estimateTokens(m))
                    .build());
        }
        return list;
    }

    private double estimateTokens(Message m) {
        String content = MessageUtils.extractText(m);
        // 粗略估算：中文约2字/1token，英文约4字/1token，这里统一按 2 字符/token
        return Math.ceil(content.length() / 2.0);
    }

    // 便捷方法：允许外部在需要时持久化长期记忆
    public MemoryChunk saveLongTermText(String sessionId, String knowledgeId, String agentId, String title, String text,
                                        List<String> tags, Double importance) {
        if (knowledgeId == null || knowledgeId.isBlank()) {
            log.warn("saveLongTermText: knowledgeId 为空，跳过长期记忆写入");
            return null;
        }
        String modelId = props.getLtm().getDefaultEmbeddingModelId();
        longTermMemoryService.saveText(sessionId, knowledgeId, agentId, title, text, tags, importance, modelId, props.getLtm().getChunkSize(), props.getLtm().getOverlap());
        return null; // 如果需要返回主 chunk，可在 LongTermMemoryService 中返回第一个/汇总
    }
}



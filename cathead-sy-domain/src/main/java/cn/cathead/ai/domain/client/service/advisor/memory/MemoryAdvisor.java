package cn.cathead.ai.domain.client.service.advisor.memory;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.IMemoryManager;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MemoryContextHolder;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.config.MemoryProperties;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 1.0.1 BaseAdvisor：在 before 注入 ST+LT 上下文，在 after 更新短期并触发长期写入。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemoryAdvisor implements BaseAdvisor {

    private static final String ATTR_SESSION_ID = "x-session-id";
    private static final String ATTR_KNOWLEDGE_ID = "x-knowledge-id";
    private static final String ATTR_AGENT_ID = "x-agent-id";

    private final IMemoryManager memoryManager;
    private final MemoryProperties props;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String sessionId = getAttribute(chatClientRequest, ATTR_SESSION_ID);
        String knowledgeId = getAttribute(chatClientRequest, ATTR_KNOWLEDGE_ID);
        String agentId = getAttribute(chatClientRequest, ATTR_AGENT_ID);
        if (sessionId == null) sessionId = MemoryContextHolder.getSessionId();
        if (knowledgeId == null) knowledgeId = MemoryContextHolder.getKnowledgeId();
        if (agentId == null) agentId = MemoryContextHolder.getAgentId();

        List<Message> originalPromptMessages = getPromptMessagesFromRequest(chatClientRequest);
        String queryText = extractLastUserText(originalPromptMessages);

        // 短期上下文
        List<Message> shortTerm = sessionId == null ? List.of() : memoryManager.getContext(sessionId);

        // 长期召回（容错：embedding 调用问题不影响主链路）
        int topK = props.getLtm().getDefaultTopK();
        List<MemoryChunk> chunks;
        try {
            chunks = memoryManager.retrieveLongTerm(knowledgeId, agentId, queryText, topK);
        } catch (Exception e) {
            chunks = List.of();
        }
        List<Message> longTerm = new ArrayList<>();
        if (!chunks.isEmpty()) {
            StringBuilder context = new StringBuilder();
            context.append("[Retrieved Long-Term Memory]\n");
            for (MemoryChunk c : chunks) {
                context.append("- ")
                        .append(c.getTitle() == null ? "chunk" : c.getTitle())
                        .append(": ")
                        .append(c.getSummary())
                        .append('\n');
            }
            longTerm.add(new AssistantMessage(context.toString()));
        }

        // 合并：LT + ST + 原始 prompt 消息
        List<Message> merged = new ArrayList<>(longTerm.size() + shortTerm.size() + originalPromptMessages.size());
        merged.addAll(longTerm);
        merged.addAll(shortTerm);
        merged.addAll(originalPromptMessages);

        // 构造新的 Prompt
        Prompt newPrompt = Prompt.builder().messages(merged).build();

        // 用 builder 模式返回新请求，context 为 Map
        return ChatClientRequest.builder()
                .prompt(newPrompt)
                .context(getContextMap(chatClientRequest))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        try {
            Object req = getRequestFromResponse(chatClientResponse);
            if (req == null) return chatClientResponse;
            String sessionId = getAttributeFromRequestObject(req, ATTR_SESSION_ID);
            if (sessionId == null) return chatClientResponse;

            List<Message> requestMessages = getPromptMessagesFromRequestObject(req);
            String userText = extractLastUserText(requestMessages);
            String assistantText = extractResponseText(chatClientResponse);

            List<Message> turn = new ArrayList<>(2);
            if (userText != null && !userText.isEmpty()) {
                turn.add(UserMessage.builder().text(userText).build());
            }
            if (assistantText != null && !assistantText.isEmpty()) {
                turn.add(new AssistantMessage(assistantText));
            }

            if (!turn.isEmpty()) {
                memoryManager.updateMemory(sessionId, turn);
            }
        } catch (Exception e) {
            log.warn("LongTermMemoryAdvisor.after failed: {}", e.getMessage());
        }
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    // -------- helper methods --------

    private static String getAttribute(ChatClientRequest req, String key) {
        try {
            Method m = req.getClass().getMethod("getAttributes");
            Object obj = m.invoke(req);
            if (obj instanceof Map<?, ?> map) {
                Object val = map.get(key);
                return val == null ? null : String.valueOf(val);
            }
        } catch (Exception ignore) { }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getContextMap(ChatClientRequest req) {
        try {
            Method m = req.getClass().getMethod("getContext");
            Object obj = m.invoke(req);
            if (obj instanceof Map<?, ?>) {
                return (Map<String, Object>) obj;
            }
        } catch (Exception ignore) { }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Message> getPromptMessagesFromRequest(ChatClientRequest req) {
        try {
            Method mPrompt = req.getClass().getMethod("getPrompt");
            Object prompt = mPrompt.invoke(req);
            if (prompt != null) {
                Method mMsgs = prompt.getClass().getMethod("getMessages");
                Object msgs = mMsgs.invoke(prompt);
                if (msgs instanceof List<?>) {
                    return (List<Message>) msgs;
                }
            }
        } catch (Exception ignore) { }
        return List.of();
    }

    private static Object getRequestFromResponse(ChatClientResponse resp) {
        for (String methodName : new String[]{"getRequest", "getChatClientRequest"}) {
            try {
                Method m = resp.getClass().getMethod(methodName);
                return m.invoke(resp);
            } catch (Exception ignore) { }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Message> getPromptMessagesFromRequestObject(Object reqObj) {
        try {
            Method mPrompt = reqObj.getClass().getMethod("getPrompt");
            Object prompt = mPrompt.invoke(reqObj);
            if (prompt != null) {
                Method mMsgs = prompt.getClass().getMethod("getMessages");
                Object msgs = mMsgs.invoke(prompt);
                if (msgs instanceof List<?>) {
                    return (List<Message>) msgs;
                }
            }
        } catch (Exception ignore) { }
        return List.of();
    }

    private static String getAttributeFromRequestObject(Object reqObj, String key) {
        try {
            Method m = reqObj.getClass().getMethod("getAttributes");
            Object obj = m.invoke(reqObj);
            if (obj instanceof Map<?, ?> map) {
                Object val = map.get(key);
                return val == null ? null : String.valueOf(val);
            }
        } catch (Exception ignore) { }
        return null;
    }

    private static String extractLastUserText(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if (m instanceof UserMessage) {
                return MessageUtils.extractText(m);
            }
        }
        return "";
    }

    private static String extractResponseText(ChatClientResponse resp) {
        try {
            Method m1 = resp.getClass().getMethod("getResults");
            Object listObj = m1.invoke(resp);
            if (listObj instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                Method m2 = first.getClass().getMethod("getOutput");
                Object output = m2.invoke(first);
                Method m3 = output.getClass().getMethod("getText");
                Object text = m3.invoke(output);
                return text == null ? "" : String.valueOf(text);
            }
        } catch (Exception ignore) { }
        return resp.toString();
    }
}



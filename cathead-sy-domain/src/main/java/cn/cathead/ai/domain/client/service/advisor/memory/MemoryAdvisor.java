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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
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
    private static final String ATTR_USE_STM = "x-use-stm";      // true/false，默认 true
    private static final String ATTR_USE_LTM = "x-use-ltm";      // true/false，默认 true
    private static final String ATTR_LTM_TOPK = "x-ltm-topk";    // int，默认 props.ltm.defaultTopK

    private final IMemoryManager memoryManager;
    private final MemoryProperties props;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = chatClientRequest.context() == null ? Map.of() : chatClientRequest.context();
        String sessionId = context.get(ATTR_SESSION_ID) == null ? null : String.valueOf(context.get(ATTR_SESSION_ID));
        String knowledgeId = context.get(ATTR_KNOWLEDGE_ID) == null ? null : String.valueOf(context.get(ATTR_KNOWLEDGE_ID));
        String agentId = context.get(ATTR_AGENT_ID) == null ? null : String.valueOf(context.get(ATTR_AGENT_ID));
        if (sessionId == null) sessionId = MemoryContextHolder.getSessionId();
        if (knowledgeId == null) knowledgeId = MemoryContextHolder.getKnowledgeId();
        if (agentId == null) agentId = MemoryContextHolder.getAgentId();

        // 原始全量消息（包含 system/assistant/user），用于保持 MCP/工具链所需的系统指令
        List<Message> originalAllMessages = chatClientRequest.prompt().getInstructions();
        // 仅用于提取用户查询文本
        List<UserMessage> originalUserMessages = chatClientRequest.prompt().getUserMessages();

        String queryText = extractLastUserText(originalUserMessages);

        boolean useStm = getBoolean(context.get(ATTR_USE_STM), true);
        boolean useLtm = getBoolean(context.get(ATTR_USE_LTM), true);
        int topK = getInt(context.get(ATTR_LTM_TOPK), props.getLtm().getDefaultTopK());

        // 短期上下文（可开关）
        List<Message> shortTerm = (useStm && sessionId != null) ? memoryManager.getContext(sessionId) : List.of();

        // 长期召回（容错：embedding 调用问题不影响主链路）
        List<MemoryChunk> chunks;
        try {
            chunks = useLtm ? memoryManager.retrieveLongTerm(knowledgeId, agentId, queryText, topK) : List.of();
        } catch (Exception e) {
            chunks = List.of();
        }
        List<Message> longTerm = new ArrayList<>();
        if (!chunks.isEmpty()) {
            StringBuilder ltmContext = new StringBuilder();
            ltmContext.append("[Retrieved Long-Term Memory]\n");
            for (MemoryChunk c : chunks) {
                ltmContext.append("- ")
                        .append(c.getTitle() == null ? "chunk" : c.getTitle())
                        .append(": ")
                        .append(c.getSummary())
                        .append('\n');
            }
            // 重要：用 SystemMessage 注入检索上下文，避免在最后一个 UserMessage 之前插入 Assistant 角色导致工具调用判定异常
            longTerm.add(new SystemMessage(ltmContext.toString()));
        }

        // 合并：将 LT/ST 插入到“最后一个用户消息”之前，确保系统/工具注入消息仍在最前
        List<Message> merged = new ArrayList<>(longTerm.size() + shortTerm.size() + originalAllMessages.size());
        int lastUserIdx = -1;
        for (int i = 0; i < originalAllMessages.size(); i++) {
            if (originalAllMessages.get(i) instanceof UserMessage) {
                lastUserIdx = i;
            }
        }
        if (lastUserIdx >= 0) {
            // 1) 保持原有顺序直至最后一个 UserMessage 之前
            if (lastUserIdx > 0) {
                merged.addAll(originalAllMessages.subList(0, lastUserIdx));
            }
            // 2) 插入 LT/ST 上下文
            merged.addAll(longTerm);
            merged.addAll(shortTerm);
            // 3) 最后一个 UserMessage
            merged.add(originalAllMessages.get(lastUserIdx));
            // 4) 其后的消息（通常无，但为稳妥保留）
            if (lastUserIdx + 1 < originalAllMessages.size()) {
                merged.addAll(originalAllMessages.subList(lastUserIdx + 1, originalAllMessages.size()));
            }
        } else {
            // 没有用户消息，退化为：LT/ST + 原消息
            merged.addAll(longTerm);
            merged.addAll(shortTerm);
            merged.addAll(originalAllMessages);
        }

        // 优先：尝试原位修改 Prompt 的指令列表，避免替换 ChatClientRequest
        try {
            Prompt origPrompt = chatClientRequest.prompt();
            Field instrField = findField(origPrompt.getClass(), "instructions");
            if (instrField == null) instrField = findField(origPrompt.getClass(), "messages");
            if (instrField != null) {
                instrField.setAccessible(true);
                instrField.set(origPrompt, merged);
                return chatClientRequest;
            }
        } catch (Throwable ignore) { }

        // 构造新的 Prompt（仅当原位修改失败时）
        Prompt newPrompt = Prompt.builder().messages(merged).build();

        // 次优先：通过反射直接替换原请求的 prompt，避免丢失工具配置
        try {
            Field promptField = findField(chatClientRequest.getClass(), "prompt");
            if (promptField != null) {
                promptField.setAccessible(true);
                promptField.set(chatClientRequest, newPrompt);
                return chatClientRequest;
            }
        } catch (Throwable ignore) { }

        // 回退：新建请求（可能丢失工具配置，仅作为兜底）
        return ChatClientRequest.builder()
                .prompt(newPrompt)
                .context(context)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        try {
            Object req = getRequestFromResponse(chatClientResponse);
            String sessionId = MemoryContextHolder.getSessionId();
            if (sessionId == null && req instanceof ChatClientRequest r) {
                Map<String, Object> ctx = r.context();
                if (ctx != null && ctx.get(ATTR_SESSION_ID) != null) {
                    sessionId = String.valueOf(ctx.get(ATTR_SESSION_ID));
                }
            }
            if (sessionId == null) return chatClientResponse;

            List<UserMessage> requestMessages = (req instanceof ChatClientRequest r)
                    ? r.prompt().getUserMessages()
                    : List.of();
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

    private static Object getRequestFromResponse(ChatClientResponse resp) {
        for (String methodName : new String[]{"getRequest", "getChatClientRequest", "request", "chatClientRequest"}) {
            try {
                Method m = resp.getClass().getMethod(methodName);
                return m.invoke(resp);
            } catch (Exception ignore) { }
        }
        return null;
    }

    private static String extractLastUserText(List<UserMessage> messages) {
        if (messages == null || messages.isEmpty()) return "";
        UserMessage m = messages.get(messages.size() - 1);
        return MessageUtils.extractText(m);
    }

    private static String extractResponseText(ChatClientResponse resp) {
        try {
            Method m1 = findNoArgMethod(resp.getClass(), "getResults", "results");
            if (m1 == null) return resp.toString();
            Object listObj = m1.invoke(resp);
            if (listObj instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                Method m2 = findNoArgMethod(first.getClass(), "getOutput", "output");
                if (m2 == null) return resp.toString();
                Object output = m2.invoke(first);
                Method m3 = findNoArgMethod(output.getClass(), "getText", "text");
                if (m3 == null) return resp.toString();
                Object text = m3.invoke(output);
                return text == null ? "" : String.valueOf(text);
            }
        } catch (Exception ignore) { }
        return resp.toString();
    }

    private static Method findNoArgMethod(Class<?> targetClass, String... candidateNames) {
        for (String name : candidateNames) {
            // 1) public 方法（包含父类/接口）
            try {
                Method m = targetClass.getMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) { }

            // 2) 在类层次结构中找声明方法（非 public）
            for (Class<?> c = targetClass; c != null; c = c.getSuperclass()) {
                try {
                    Method m = c.getDeclaredMethod(name);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) { }
            }

            // 3) 在所有接口及其父接口中找声明方法
            Method m = findInInterfaces(targetClass, name);
            if (m != null) return m;
        }
        return null;
    }

    private static Field findField(Class<?> targetClass, String name) {
        for (Class<?> c = targetClass; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) { }
        }
        return null;
    }

    private static boolean getBoolean(Object v, boolean defaultValue) {
        if (v == null) return defaultValue;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)) return false;
        return defaultValue;
    }

    private static int getInt(Object v, int defaultValue) {
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    private static Method findInInterfaces(Class<?> targetClass, String name) {
        for (Class<?> itf : targetClass.getInterfaces()) {
            try {
                Method m = itf.getMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) { }
            try {
                Method m = itf.getDeclaredMethod(name);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) { }
            Method deeper = findInInterfaces(itf, name);
            if (deeper != null) return deeper;
        }
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass != null) {
            Method fromSuper = findInInterfaces(superClass, name);
            if (fromSuper != null) return fromSuper;
        }
        return null;
    }
}



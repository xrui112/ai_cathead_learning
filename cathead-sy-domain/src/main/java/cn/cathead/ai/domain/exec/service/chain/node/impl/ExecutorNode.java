package cn.cathead.ai.domain.exec.service.chain.node.impl;

import cn.cathead.ai.domain.client.service.advisor.memory.manager.IMemoryManager;
import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.service.chain.loop.LoopChain;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import cn.cathead.ai.domain.exec.service.chain.node.LoopNode;
import cn.cathead.ai.domain.model.service.registry.ModelProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.Message;

import static cn.cathead.ai.domain.exec.service.chain.tools.SseUtils.sendSection;
import static cn.cathead.ai.domain.exec.service.chain.tools.PlanConstants.PLAN_START;
import static cn.cathead.ai.domain.exec.service.chain.tools.PlanConstants.PLAN_END;

import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MessageUtils;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExecutorNode implements LoopNode {

    public static final String NAME = "Executor";

    @Override
    public String getName() {
        return NAME;
    }

    private final IMemoryManager memoryManager;
    private final List<ToolCallback> toolCallbacks;

    private final ModelProviderService modelProviderService;


    @Override
    public void handle(LoopContext ctx, ChainContext chainContext, LoopChain chain) {
        if (ctx.isCompleted() || ctx.getStep() > ctx.getMaxStep()) {
            chain.jumpTo(SummaryNode.NAME, ctx, chainContext);
            return;
        }
        String sessionId = String.valueOf(chainContext.getParams().get("sessionId"));
        
        // 从记忆中提取执行计划
        String planJson = extractPlanFromMemory(sessionId);
        String prompt = buildPrompt(ctx, planJson);
        
        log.info("[ExecutorNode] 开始执行，工具数: {}", toolCallbacks == null ? 0 : toolCallbacks.size());
        if (planJson != null) {
            log.info("[ExecutorNode] ✅ 找到执行计划: {}", planJson.length() > 200 ? planJson.substring(0, 200) + "..." : planJson);
        } else {
            log.warn("[ExecutorNode] ⚠️ 未找到执行计划");
        }
        sendSection(ctx, NAME, "EXECUTION_START", "开始执行任务");

        try {

            ChatClient client = chainContext.getChatClient();

            // 配置工具调用选项：和测试一样使用 required
            org.springframework.ai.openai.OpenAiChatOptions toolOptions = 
                org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .streamUsage(true)
                    .toolChoice("required")  // 强制必须调用工具（和测试保持一致）
                    .build();


            ChatResponse response = client.prompt(prompt)
                    .advisors(a -> a
                        .param("x-session-id", chainContext.getParams().get("sessionId"))
                        .param("x-agent-id", chainContext.getParams().get("agentId"))
                        .param("x-knowledge-id", chainContext.getParams().get("knowledgeId"))
                    )
                    .options(toolOptions)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .chatResponse();
            

            String content = "";
            if (response != null && !response.getResults().isEmpty()) {
                Generation generation = response.getResults().get(0);
                if (generation != null && generation.getOutput() != null) {
                    content = generation.getOutput().getText();
                }
                
                String finishReason = generation != null && generation.getMetadata() != null 
                    ? generation.getMetadata().getFinishReason() : "unknown";
                log.info("[ExecutorNode] 执行完成，finishReason={}, 结果长度={}", finishReason, content.length());
            }
            
            // 保存到记忆并发送
            memoryManager.saveShortTermTextAsAssistant(sessionId, content);
            sendSection(ctx, NAME, "EXECUTION_RESULT", content);
            
        } catch (Exception e) {
            log.error("[ExecutorNode] 执行异常", e);
            sendSection(ctx, NAME, "EXECUTION_ERROR", "执行出错: " + e.getMessage());
        }
        
        chain.jumpTo(SupervisorNode.NAME, ctx, chainContext);
    }

    private String buildPrompt(LoopContext ctx, String planJson) {
        StringBuilder sb = new StringBuilder();
        
        if (planJson != null && !planJson.isBlank()) {
            sb.append("执行以下计划：\n");
            sb.append(planJson).append("\n\n");
        }
        
        sb.append("可用工具：\n");
        sb.append("- sql_query(datasourceId, sql): 执行SQL查询\n");
        sb.append("- data_process(rows, ops): 处理数据\n");
        sb.append("- viz_build(rows, vizSpec): 生成图表\n\n");
        
        if (planJson != null && !planJson.isBlank()) {
            sb.append("请根据计划中的 SQL 和 datasourceId 调用 sql_query 工具。");
        } else {
            sb.append("请根据记忆中的任务调用合适的工具完成任务。");
        }
        
        return sb.toString();
    }
    
    /**
     * 从记忆中提取执行计划
     */
    private String extractPlanFromMemory(String sessionId) {
        try {
            List<Message> ctxMessages = memoryManager.getContext(sessionId);
            if (ctxMessages != null && !ctxMessages.isEmpty()) {
                for (int i = ctxMessages.size() - 1; i >= 0; i--) {
                    String txt = MessageUtils.extractText(ctxMessages.get(i));
                    int s = txt.lastIndexOf(PLAN_START);
                    int e = txt.lastIndexOf(PLAN_END);
                    if (s >= 0 && e > s) {
                        return txt.substring(s + PLAN_START.length(), e).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.error("[ExecutorNode] 提取计划失败", e);
        }
        return null;
    }
    
    /**
     * 手动解析并执行工具（用于 ReAct 格式）
     */
    private String manualExecuteTool(String textContent, List<ToolCallback> callbacks) {
        try {
            log.info("[ExecutorNode] 开始手动解析工具调用...");
            log.info("[ExecutorNode] 原文: {}", textContent.substring(0, Math.min(500, textContent.length())));
            
            // 提取 JSON：tool_calls: {...} 或 直接的 {"name": "sql_query", ...}
            String jsonStr = null;
            
            // 方式1: tool_calls: {...}
            if (textContent.contains("tool_calls:")) {
                int start = textContent.indexOf("tool_calls:") + 11;
                int jsonStart = textContent.indexOf("{", start);
                if (jsonStart >= 0) {
                    int braceCount = 0;
                    int jsonEnd = -1;
                    for (int i = jsonStart; i < textContent.length(); i++) {
                        if (textContent.charAt(i) == '{') braceCount++;
                        else if (textContent.charAt(i) == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                jsonEnd = i + 1;
                                break;
                            }
                        }
                    }
                    if (jsonEnd > jsonStart) {
                        jsonStr = textContent.substring(jsonStart, jsonEnd);
                    }
                }
            }
            
            if (jsonStr != null) {
                log.info("[ExecutorNode] 提取到 JSON: {}", jsonStr);
                
                // 调用工具
                for (ToolCallback callback : callbacks) {
                    if (callback.toString().contains("name=sql_query")) {
                        log.info("[ExecutorNode] 调用 sql_query 工具...");
                        
                        // 解析 JSON 获取 arguments
                        com.fasterxml.jackson.databind.ObjectMapper mapper = 
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        var toolCall = mapper.readValue(jsonStr, java.util.Map.class);
                        var arguments = toolCall.get("arguments");
                        
                        String argsJson = mapper.writeValueAsString(arguments);
                        log.info("[ExecutorNode] 参数: {}", argsJson);
                        
                        String result = callback.call(argsJson);
                        log.info("[ExecutorNode] 🔥🔥🔥 执行成功！");
                        
                        return "查询结果：\n" + result;
                    }
                }
            }
            
            log.warn("[ExecutorNode] 无法解析或执行，返回原文本");
            return textContent;
            
        } catch (Exception e) {
            log.error("[ExecutorNode] 手动执行失败", e);
            return "执行失败: " + e.getMessage();
        }
    }
}



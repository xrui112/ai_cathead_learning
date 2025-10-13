package cn.cathead.ai.domain.exec.service.chain.node.impl;

import cn.cathead.ai.domain.client.service.advisor.memory.manager.IMemoryManager;
import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.service.chain.loop.LoopChain;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import cn.cathead.ai.domain.exec.service.chain.node.LoopNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import static cn.cathead.ai.domain.exec.service.chain.tools.SseUtils.sendSection;
import static cn.cathead.ai.domain.exec.service.chain.tools.PlanConstants.PLAN_START;
import static cn.cathead.ai.domain.exec.service.chain.tools.PlanConstants.PLAN_END;

@Slf4j
@RequiredArgsConstructor
@Component
public class AnalyzerNode implements LoopNode {

    public static final String NAME = "Analyzer";

    @Override
    public String getName() {
        return NAME;
    }

    private final IMemoryManager memoryManager;

    @Override
    public void handle(LoopContext ctx, ChainContext chainContext, LoopChain chain) {
        if (ctx.isCompleted() || ctx.getStep() > ctx.getMaxStep()) {
            chain.jumpTo(SummaryNode.NAME, ctx, chainContext);
            return;
        }
        String prompt = buildPrompt(ctx);
        sendSection(ctx, NAME, "PROMPT", prompt);

        ChatClient client = chainContext.getChatClient();
        String content = client.prompt(prompt)
                .advisors(a -> a
                        .param("x-session-id", chainContext.getParams().get("sessionId"))
                        .param("x-agent-id", chainContext.getParams().get("agentId"))
                        .param("x-knowledge-id", chainContext.getParams().get("knowledgeId"))
                )
                .call()
                .content();

        String sessionId = String.valueOf(chainContext.getParams().get("sessionId"));
        memoryManager.saveShortTermTextAsAssistant(sessionId, content);
        sendSection(ctx, NAME, "ANALYSIS", content);

        boolean hasPlan = content.contains(PLAN_START) && content.contains(PLAN_END);
        log.info("[AnalyzerNode] 分析完成，包含执行计划: {}", hasPlan);

        if (!hasPlan) {
            log.warn("[AnalyzerNode] ⚠️ 分析结果未包含计划，尝试追加计划请求");
            String planHint = "请基于任务产出严格JSON计划，且仅输出在" + PLAN_START + "与" + PLAN_END + "之间。" +
                    "JSON结构: {\"steps\":[{\"type\":\"data_query\",\"source\":{\"kind\":\"sql\",\"datasourceId\":\"bi_mysql_rw\"},\"intent\":\"...\",\"constraints\":{\"timeRange\":[\"YYYY-MM-DD\",\"YYYY-MM-DD\"]}},{\"type\":\"processing\",\"ops\":[{\"op\":\"group_by\",\"keys\":[\"month\"]},{\"op\":\"sum\",\"field\":\"revenue\",\"as\":\"revenue\"}]},{\"type\":\"viz\",\"chart\":\"bar\",\"x\":\"month\",\"series\":{\"key\":\"product_line\",\"value\":\"revenue\"}}]}";
            String planPrompt = prompt + "\n\n" + planHint;
            sendSection(ctx, NAME, "PROMPT", planPrompt);
            String planContent = chainContext.getChatClient().prompt(planPrompt)
                    .advisors(a -> a
                            .param("x-session-id", chainContext.getParams().get("sessionId"))
                            .param("x-agent-id", chainContext.getParams().get("agentId"))
                            .param("x-knowledge-id", chainContext.getParams().get("knowledgeId"))
                    )
                    .call()
                    .content();
            if (planContent != null && !planContent.isBlank()) {
                memoryManager.saveShortTermTextAsAssistant(sessionId, planContent);
                sendSection(ctx, NAME, "PLAN", planContent);
                
                boolean hasRetryPlan = planContent.contains(PLAN_START) && planContent.contains(PLAN_END);
                log.info("[AnalyzerNode] 追加请求完成，包含计划: {}", hasRetryPlan);
            }
        } else {
            log.info("[AnalyzerNode] ✅ 已生成执行计划，将跳转到 ExecutorNode");
        }
        if (isCompletedByText(content)) {
            ctx.setCompleted(true);
            log.info("[AnalyzerNode] 任务已完成，将跳转到 SummaryNode");
        }
        String nextNode = ctx.isCompleted() || ctx.getStep() > ctx.getMaxStep() ? SummaryNode.NAME : ExecutorNode.NAME;
        log.info("[AnalyzerNode] 跳转到: {}", nextNode);
        chain.jumpTo(nextNode, ctx, chainContext);
    }

    private String buildPrompt(LoopContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前任务】\n").append(ctx.getCurrentTask()).append("\n\n");
        
        sb.append("请分析任务并提供：\n");
        sb.append("1. 任务理解和分解\n");
        sb.append("2. 完成度评估 (0-100%)\n");
        sb.append("3. 任务状态 (COMPLETED/ONGOING)\n");
        sb.append("4. 执行策略建议\n\n");
        
        sb.append("【执行计划】\n");
        sb.append("如果任务需要具体执行（如数据查询、API调用、计算等），请制定执行计划：\n");
        sb.append("- 计划使用 JSON 格式\n");
        sb.append("- 必须在 ").append(PLAN_START).append(" 和 ").append(PLAN_END).append(" 标记之间\n");
        sb.append("- 根据任务类型自由设计计划结构\n");
        sb.append("- 包含足够的细节信息供后续执行\n\n");
        
        sb.append("常见任务类型示例：\n");
        sb.append("• 数据查询/统计：包含 sql、datasourceId 等信息\n");
        sb.append("• 数据处理：包含 data_source、operations 等信息\n");
        sb.append("• 可视化：包含 chart_type、data_config 等信息\n");
        sb.append("• API调用：包含 endpoint、method、params 等信息\n\n");
        
        sb.append("示例（数据查询任务）：\n");
        sb.append(PLAN_START).append("\n");
        sb.append("{\n");
        sb.append("  \"task_type\": \"data_query\",\n");
        sb.append("  \"data_query\": {\n");
        sb.append("    \"sql\": \"完整的SQL语句\",\n");
        sb.append("    \"source\": {\"datasourceId\": \"mysqlDataSource\"}\n");
        sb.append("  },\n");
        sb.append("  \"processing\": {\"可选的数据处理步骤\": \"...\"},\n");
        sb.append("  \"output\": {\"expected_format\": \"table/chart/text\"}\n");
        sb.append("}\n");
        sb.append(PLAN_END).append("\n\n");
        
        sb.append("注意：根据实际任务灵活调整计划结构，不必拘泥于示例格式。");
        
        return sb.toString();
    }

    private boolean isCompletedByText(String text) {
        if (text == null) return false;
        String t = text.toUpperCase();
        return t.contains("任务状态: COMPLETED") || t.contains("完成度评估: 100%")
                || t.contains("TASK STATUS: COMPLETED") || t.contains("COMPLETION: 100%");
    }

}



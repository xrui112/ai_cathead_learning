package cn.cathead.ai.domain.exec.service.chain.node.impl;

import cn.cathead.ai.domain.exec.model.entity.ExecutionRecord;
import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.service.chain.loop.LoopChain;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import cn.cathead.ai.domain.exec.service.chain.node.LoopNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import static cn.cathead.ai.domain.exec.service.chain.tools.SseUtils.sendSection;

@Slf4j
public class AnalyzerNode implements LoopNode {

    public static final String NAME = "Analyzer";

    @Override
    public String getName() {
        return NAME;
    }

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

        ctx.setAnalysisResult(content);
        sendSection(ctx, NAME, "ANALYSIS", content);

        if (isCompletedByText(content)) {
            ctx.setCompleted(true);
        }

        ensureHistory(ctx);
        chain.jumpTo(ctx.isCompleted() || ctx.getStep() > ctx.getMaxStep() ? SummaryNode.NAME : ExecutorNode.NAME, ctx, chainContext);
    }

    private String buildPrompt(LoopContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Task]\n").append(ctx.getCurrentTask()).append('\n');
        sb.append("[History]\n");
        ctx.getExecutionHistory().forEach(r -> sb.append("- step ").append(r.getStep()).append(": ")
                .append(r.getExecutionResult() == null ? "" : r.getExecutionResult()).append('\n'));
        sb.append("请分析任务完成路径，给出完成度评估(0-100%)与任务状态(COMPLETED/ONGOING)，并提供执行策略。");
        return sb.toString();
    }

    private boolean isCompletedByText(String text) {
        if (text == null) return false;
        String t = text.toUpperCase();
        return t.contains("任务状态: COMPLETED") || t.contains("完成度评估: 100%")
                || t.contains("TASK STATUS: COMPLETED") || t.contains("COMPLETION: 100%");
    }

    private void ensureHistory(LoopContext ctx) {
        if (ctx.getExecutionHistory().isEmpty() || ctx.getExecutionHistory().get(ctx.getExecutionHistory().size() - 1).getStep() != ctx.getStep()) {
            ctx.getExecutionHistory().add(ExecutionRecord.builder()
                    .step(ctx.getStep())
                    .analysisResult(ctx.getAnalysisResult())
                    .build());
        } else {
            ctx.getExecutionHistory().get(ctx.getExecutionHistory().size() - 1)
                    .setAnalysisResult(ctx.getAnalysisResult());
        }
    }
}



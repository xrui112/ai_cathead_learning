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
public class SummaryNode implements LoopNode {

    public static final String NAME = "Summary";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void handle(LoopContext ctx, ChainContext chainContext, LoopChain chain) {
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

        ctx.setFinalSummary(content);
        sendSection(ctx, NAME, "SUMMARY", content);
        sendSection(ctx, NAME, "DONE", ctx.isCompleted() ? "COMPLETED" : "MAX_STEP_REACHED");

        ensureHistory(ctx);
        chain.end(ctx, chainContext);
    }

    private String buildPrompt(LoopContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下执行历史生成最终报告：\n");
        for (ExecutionRecord r : ctx.getExecutionHistory()) {
            sb.append("Step ").append(r.getStep()).append(':').append('\n');
            if (r.getAnalysisResult() != null) sb.append("- Analysis: ").append(r.getAnalysisResult()).append('\n');
            if (r.getExecutionResult() != null) sb.append("- Execution: ").append(r.getExecutionResult()).append('\n');
            if (r.getSupervisionResult() != null) sb.append("- Supervision: ").append(r.getSupervisionResult()).append('\n');
        }
        return sb.toString();
    }

    private void ensureHistory(LoopContext ctx) {
        if (ctx.getExecutionHistory().isEmpty()) return;
        ExecutionRecord last = ctx.getExecutionHistory().get(ctx.getExecutionHistory().size() - 1);
        if (last.getStep() != ctx.getStep()) {
            ctx.getExecutionHistory().add(ExecutionRecord.builder().step(ctx.getStep()).build());
        }
    }
}



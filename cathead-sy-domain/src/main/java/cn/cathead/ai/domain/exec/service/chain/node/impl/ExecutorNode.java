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
public class ExecutorNode implements LoopNode {

    public static final String NAME = "Executor";

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

        ctx.setExecutionResult(content);
        sendSection(ctx, NAME, "EXECUTION", content);

        ensureHistory(ctx);
        chain.jumpTo(SupervisorNode.NAME, ctx, chainContext);
    }

    private String buildPrompt(LoopContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Analysis]\n").append(ctx.getAnalysisResult()).append('\n');
        sb.append("请基于分析制定并执行一步可操作的行动，输出：目标/过程/结果/质量检查。");
        return sb.toString();
    }

    private void ensureHistory(LoopContext ctx) {
        if (ctx.getExecutionHistory().isEmpty() || ctx.getExecutionHistory().get(ctx.getExecutionHistory().size() - 1).getStep() != ctx.getStep()) {
            ctx.getExecutionHistory().add(ExecutionRecord.builder()
                    .step(ctx.getStep())
                    .executionResult(ctx.getExecutionResult())
                    .build());
        } else {
            ctx.getExecutionHistory().get(ctx.getExecutionHistory().size() - 1)
                    .setExecutionResult(ctx.getExecutionResult());
        }
    }
}



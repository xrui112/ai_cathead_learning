package cn.cathead.ai.domain.exec.service.chain.factory.node.impl;

import cn.cathead.ai.domain.exec.model.entity.ExecutionRecord;
import cn.cathead.ai.domain.exec.service.chain.factory.context.ChainContext;
import cn.cathead.ai.domain.exec.service.chain.factory.loop.LoopChain;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import cn.cathead.ai.domain.exec.service.chain.factory.node.LoopNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import static cn.cathead.ai.domain.exec.service.chain.tools.SseUtils.sendSection;

@Slf4j
public class SupervisorNode implements LoopNode {

    public static final String NAME = "Supervisor";

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

        ctx.setSupervisionResult(content);
        sendSection(ctx, NAME, "REVIEW", content);

        String upper = content == null ? "" : content.toUpperCase();
        if (upper.contains("PASS")) {
            ctx.setCompleted(true);
        } else if (upper.contains("FAIL") || upper.contains("OPTIMIZE")) {
            ctx.setCurrentTask("优化执行以满足评审意见：" + ctx.getCurrentTask());
        }

        ctx.setStep(ctx.getStep() + 1);

        ensureHistory(ctx);
        chain.jumpTo(ctx.isCompleted() || ctx.getStep() > ctx.getMaxStep() ? SummaryNode.NAME : AnalyzerNode.NAME, ctx, chainContext);
    }

    private String buildPrompt(LoopContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Execution]\n").append(ctx.getExecutionResult()).append('\n');
        sb.append("请对结果进行评分与是否通过(PASS/FAIL/OPTIMIZE)，并给出改进建议。");
        return sb.toString();
    }

    private void ensureHistory(LoopContext ctx) {
        if (ctx.getExecutionHistory().isEmpty() || ctx.getExecutionHistory().get(ctx.getExecutionHistory().size() - 1).getStep() != ctx.getStep() - 1) {
            ctx.getExecutionHistory().add(ExecutionRecord.builder()
                    .step(ctx.getStep() - 1)
                    .supervisionResult(ctx.getSupervisionResult())
                    .build());
        } else {
            ctx.getExecutionHistory().get(ctx.getExecutionHistory().size() - 1)
                    .setSupervisionResult(ctx.getSupervisionResult());
        }
    }
}



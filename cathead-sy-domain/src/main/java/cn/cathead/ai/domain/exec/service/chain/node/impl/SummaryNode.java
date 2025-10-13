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

@Slf4j
@RequiredArgsConstructor
@Component
public class SummaryNode implements LoopNode {

    public static final String NAME = "Summary";

    @Override
    public String getName() {
        return NAME;
    }

    private final IMemoryManager memoryManager;

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

        // 写入最终总结到短期记忆
        String sessionId = String.valueOf(chainContext.getParams().get("sessionId"));
        memoryManager.saveShortTermTextAsAssistant(sessionId, content);
        sendSection(ctx, NAME, "SUMMARY", content);
        sendSection(ctx, NAME, "DONE", ctx.isCompleted() ? "COMPLETED" : "MAX_STEP_REACHED");
        chain.end(ctx, chainContext);
    }

    private String buildPrompt(LoopContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("请基于整个会话的记忆（用户任务与各阶段输出）生成最终报告，并简要列出关键结论与后续建议。");
        return sb.toString();
    }
}



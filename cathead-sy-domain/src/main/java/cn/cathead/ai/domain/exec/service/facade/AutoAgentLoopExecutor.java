package cn.cathead.ai.domain.exec.service.facade;

import cn.cathead.ai.domain.client.service.build.IClientBuilderService;
import cn.cathead.ai.domain.exec.model.entity.AutoAgentExecuteResultEntity;
import cn.cathead.ai.domain.exec.model.entity.ExecuteCommandEntity;
import cn.cathead.ai.domain.exec.service.chain.factory.loop.LoopChain;
import cn.cathead.ai.domain.exec.service.chain.factory.ExecFactory;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import cn.cathead.ai.domain.exec.service.chain.factory.context.ChainContext;
import cn.cathead.ai.domain.exec.model.entity.Emitter;
import lombok.RequiredArgsConstructor;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.MemoryContextHolder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static cn.cathead.ai.domain.exec.service.chain.tools.SseUtils.sendSseResult;

@Service
@RequiredArgsConstructor
public class AutoAgentLoopExecutor {

    private final IClientBuilderService clientBuilderService;
    private final ExecFactory execFactory;

    public void execute(ExecuteCommandEntity cmd, Emitter<String> emitter) {
        ChatClient chatClient = clientBuilderService.build(cmd.getModelId());

        LoopContext ctx = new LoopContext();
        ctx.setStep(1);
        ctx.setMaxStep(cmd.getMaxStep() <= 0 ? 5 : cmd.getMaxStep());
        ctx.setCurrentTask(cmd.getTask());
        ctx.setEmitter(emitter);

        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", cmd.getSessionId());
        params.put("agentId", cmd.getAgentId());
        params.put("knowledgeId", cmd.getKnowledgeId());
        if (cmd.getExtraParams() != null) params.putAll(cmd.getExtraParams());

        ChainContext chainCtx = execFactory.createChainContext(chatClient, params);
        LoopChain chain = execFactory.createLoopChain();

        try {
            MemoryContextHolder.set(cmd.getSessionId(), cmd.getAgentId(), cmd.getKnowledgeId());
            chain.jumpTo("Analyzer", ctx, chainCtx);
        } finally {
            MemoryContextHolder.clear();
            sendSseResult(ctx, AutoAgentExecuteResultEntity.builder()
                    .stage("System")
                    .subType("END")
                    .content("FINISH")
                    .build());
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }
}



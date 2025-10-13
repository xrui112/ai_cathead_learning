package cn.cathead.ai.domain.exec.service.chain.factory;

import cn.cathead.ai.domain.exec.service.chain.loop.DefaultLoopChain;
import cn.cathead.ai.domain.exec.service.chain.loop.LoopChain;
import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.model.entity.DefaultChainContext;
import cn.cathead.ai.domain.exec.service.chain.node.LoopNode;
import cn.cathead.ai.domain.exec.service.chain.node.impl.AnalyzerNode;
import cn.cathead.ai.domain.exec.service.chain.node.impl.ExecutorNode;
import cn.cathead.ai.domain.exec.service.chain.node.impl.SummaryNode;
import cn.cathead.ai.domain.exec.service.chain.node.impl.SupervisorNode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultExecFactory implements ExecFactory {

    // 节点 Bean 由 Spring 管理
    private final AnalyzerNode analyzerNode;
    private final ExecutorNode executorNode;
    private final SupervisorNode supervisorNode;
    private final SummaryNode summaryNode;

    @Override
    public LoopChain createLoopChain() {
        List<LoopNode> nodes = List.of(
                analyzerNode,
                executorNode,
                supervisorNode,
                summaryNode
        );
        return new DefaultLoopChain(nodes);
    }

    @Override
    public ChainContext createChainContext(ChatClient chatClient, Map<String, Object> params) {
        return new DefaultChainContext(chatClient, params);
    }
}



package cn.cathead.ai.domain.exec.service.chain.factory;

import cn.cathead.ai.domain.exec.service.chain.factory.loop.DefaultLoopChain;
import cn.cathead.ai.domain.exec.service.chain.factory.loop.LoopChain;
import cn.cathead.ai.domain.exec.service.chain.factory.context.ChainContext;
import cn.cathead.ai.domain.exec.service.chain.factory.context.DefaultChainContext;
import cn.cathead.ai.domain.exec.service.chain.factory.node.LoopNode;
import cn.cathead.ai.domain.exec.service.chain.factory.node.impl.AnalyzerNode;
import cn.cathead.ai.domain.exec.service.chain.factory.node.impl.ExecutorNode;
import cn.cathead.ai.domain.exec.service.chain.factory.node.impl.SummaryNode;
import cn.cathead.ai.domain.exec.service.chain.factory.node.impl.SupervisorNode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultExecFactory implements ExecFactory {

    @Override
    public LoopChain createLoopChain() {
        List<LoopNode> nodes = List.of(
                new AnalyzerNode(),
                new ExecutorNode(),
                new SupervisorNode(),
                new SummaryNode()
        );
        return new DefaultLoopChain(nodes);
    }

    @Override
    public ChainContext createChainContext(ChatClient chatClient, Map<String, Object> params) {
        return new DefaultChainContext(chatClient, params);
    }
}



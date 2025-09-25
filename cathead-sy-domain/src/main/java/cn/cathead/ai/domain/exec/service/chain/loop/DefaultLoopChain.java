package cn.cathead.ai.domain.exec.service.chain.loop;

import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import cn.cathead.ai.domain.exec.service.chain.node.LoopNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认 LoopChain 实现，支持 proceed/jumpTo/end。
 */
public class DefaultLoopChain implements LoopChain {

    private final List<LoopNode> nodes;
    private final Map<String, Integer> nameIndex = new HashMap<>();

    private int index = 0;
    private final AtomicBoolean ended = new AtomicBoolean(false);

    public DefaultLoopChain(List<LoopNode> nodes) {
        this.nodes = nodes;
        for (int i = 0; i < nodes.size(); i++) {
            nameIndex.put(nodes.get(i).getName(), i);
        }
    }

    @Override
    public void proceed(LoopContext ctx, ChainContext chainContext) {
        if (ended.get()) return;
        if (index >= nodes.size()) return;
        LoopNode current = nodes.get(index++);
        try {
            current.handle(ctx, chainContext, this);
        } catch (Throwable t) {
            current.onError(t, ctx, chainContext, this);
        }
    }

    @Override
    public void jumpTo(String nodeName, LoopContext ctx, ChainContext chainContext) {
        if (ended.get()) return;
        Integer i = nameIndex.get(nodeName);
        if (i == null) return;
        index = i;
        proceed(ctx, chainContext);
    }

    @Override
    public void end(LoopContext ctx, ChainContext chainContext) {
        ended.set(true);
    }
}



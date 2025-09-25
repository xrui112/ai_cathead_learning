package cn.cathead.ai.domain.exec.service.chain.node;

import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.service.chain.loop.LoopChain;
import cn.cathead.ai.domain.exec.model.entity.LoopContext;

/**
 * 循环责任链 Node 接口，提供 before/handle/after/onError 钩子。
 */
public interface LoopNode {

    String getName();

    default void before(LoopContext ctx, ChainContext chainContext, LoopChain chain) {
        chain.proceed(ctx, chainContext);
    }

    void handle(LoopContext ctx, ChainContext chainContext, LoopChain chain);

    default void after(LoopContext ctx, ChainContext chainContext, LoopChain chain) {
        chain.proceed(ctx, chainContext);
    }

    default void onError(Throwable t, LoopContext ctx, ChainContext chainContext, LoopChain chain) {
        chain.proceed(ctx, chainContext);
    }
}



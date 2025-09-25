package cn.cathead.ai.domain.exec.service.chain.loop;

import cn.cathead.ai.domain.exec.model.entity.LoopContext;
import cn.cathead.ai.domain.exec.model.entity.ChainContext;

public interface LoopChain {

    void proceed(LoopContext ctx, ChainContext chainContext);

    void jumpTo(String nodeName, LoopContext ctx, ChainContext chainContext);

    void end(LoopContext ctx, ChainContext chainContext);
}



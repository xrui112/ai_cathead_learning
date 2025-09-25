package cn.cathead.ai.domain.exec.service.chain.factory;

import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import cn.cathead.ai.domain.exec.service.chain.loop.LoopChain;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * 负责装配循环执行链与其运行上下文。
 */
public interface ExecFactory {

    /** 创建默认 Node 顺序的 LoopChain（Analyzer -> Executor -> Supervisor -> Summary） */
    LoopChain createLoopChain();

    /** 基于 ChatClient 和参数构建链路上下文（ChainContext） */
    ChainContext createChainContext(ChatClient chatClient, Map<String, Object> params);
}



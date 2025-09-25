package cn.cathead.ai.domain.client.service.advisor.memory.manager.instant.compress;

import cn.cathead.ai.domain.client.model.entity.MemoryMessage;
import cn.cathead.ai.domain.client.model.valobj.CompressionResult;
import cn.cathead.ai.domain.client.service.advisor.memory.manager.tools.ShortTermPolicy;

import java.util.List;

/**
 * 记忆压缩器接口：将旧消息压缩为结构化摘要
 */
public interface IMemoryCompressor {
    CompressionResult compress(List<MemoryMessage> messages, ShortTermPolicy policy);
}



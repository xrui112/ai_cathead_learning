package cn.cathead.ai.domain.client.model.valobj;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import cn.cathead.ai.domain.client.model.entity.MemoryMessage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CompressionResult {
    private final List<MemoryMessage> retainedMessages;
    private final List<MemoryChunk> newChunks;
    private final String compressionRatio;
}



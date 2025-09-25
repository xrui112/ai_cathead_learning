package cn.cathead.ai.domain.client.event;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import lombok.Getter;

import java.util.List;

@Getter
public class MemoryCompressedEvent {
    private final String sessionId;
    private final String compressionRatio;
    private final List<MemoryChunk> chunks;

    public MemoryCompressedEvent(String sessionId, String compressionRatio, List<MemoryChunk> chunks) {
        this.sessionId = sessionId;
        this.compressionRatio = compressionRatio;
        this.chunks = chunks;
    }
}



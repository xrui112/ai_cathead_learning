package cn.cathead.ai.domain.client.event;

import cn.cathead.ai.domain.client.model.entity.MemoryChunk;
import lombok.Getter;

@Getter
public class MemoryPersistedEvent {
    private final String chunkId;
    private final MemoryChunk chunk;

    public MemoryPersistedEvent(String chunkId, MemoryChunk chunk) {
        this.chunkId = chunkId;
        this.chunk = chunk;
    }
}



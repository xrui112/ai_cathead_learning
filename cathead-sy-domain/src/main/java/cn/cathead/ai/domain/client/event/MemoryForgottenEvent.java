package cn.cathead.ai.domain.client.event;

import lombok.Getter;

@Getter
public class MemoryForgottenEvent {
    private final String chunkId;
    private final double importanceScore;

    public MemoryForgottenEvent(String chunkId, double importanceScore) {
        this.chunkId = chunkId;
        this.importanceScore = importanceScore;
    }
}



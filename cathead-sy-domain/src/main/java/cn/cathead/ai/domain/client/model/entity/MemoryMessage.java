package cn.cathead.ai.domain.client.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.ai.chat.messages.Message;

import java.time.Instant;

/**
 * 存储于短期/长期记忆的消息条目
 */
@Getter
@ToString
@Builder
public class MemoryMessage {
    private final String id;
    private final Message payload;
    private final Instant createdAt;
    private final double tokenCost; // 估算token开销
}



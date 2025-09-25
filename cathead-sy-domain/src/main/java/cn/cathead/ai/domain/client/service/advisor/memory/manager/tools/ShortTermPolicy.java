package cn.cathead.ai.domain.client.service.advisor.memory.manager.tools;

import lombok.Builder;
import lombok.Getter;

/**
 * 短期记忆策略：阈值、压缩比例等
 */
@Getter
@Builder
public class ShortTermPolicy {
    /** 模型上下文容量估算（token） */
    private final int contextWindowTokens;
    /** 触发压缩阈值百分比，0.8 ~ 0.9 */
    private final double compressThresholdRatio;
    /** 简单最大消息条数防护 */
    private final int maxMessages;
    /** 是否启用条数触发 */
    private final boolean enableCountTrigger;

    public boolean shouldCompress(double currentTokens, int currentCount) {
        double threshold = contextWindowTokens * compressThresholdRatio;
        boolean byToken = currentTokens >= threshold;
        boolean byCount = enableCountTrigger && currentCount > maxMessages;
        return byToken || byCount;
    }

    public static ShortTermPolicy of(int windowTokens, double ratio, int maxMessages) {
        return ShortTermPolicy.builder()
                .contextWindowTokens(windowTokens)
                .compressThresholdRatio(ratio)
                .maxMessages(maxMessages)
                .enableCountTrigger(true)
                .build();
    }
}



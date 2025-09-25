package cn.cathead.ai.domain.client.model.valobj;

import lombok.Getter;
import lombok.ToString;

/**
 * 重要性评分值对象
 * 结合启发式与业务规则对记忆条目赋予分值
 */
@Getter
@ToString
public class ImportanceScore {

    /**
     * 0.0 ~ 1.0，数值越高表示越重要
     */
    private final double score;

    /**
     * 评分原因摘要，便于审计与调试
     */
    private final String reason;

    public ImportanceScore(double score, String reason) {
        double bounded = Math.max(0.0, Math.min(1.0, score));
        this.score = bounded;
        this.reason = reason == null ? "" : reason;
    }

    public static ImportanceScore of(double score, String reason) {
        return new ImportanceScore(score, reason);
    }
}



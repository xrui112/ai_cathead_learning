package cn.cathead.ai.domain.exec.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * SSE 数据载体，统一结构：阶段、子类型、内容、时间戳、上下文快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoAgentExecuteResultEntity implements Serializable {

    /** 阶段：ANALYZER / EXECUTOR / SUPERVISOR / SUMMARY */
    private String stage;

    /** 子类型：如 SECTION、FULL、INFO、WARN、ERROR、METRIC 等 */
    private String subType;

    /** 文本内容 */
    private String content;

    /** 额外指标或结构化数据 */
    private Map<String, Object> meta;

    /** 事件时间 */
    private Instant timestamp;
}



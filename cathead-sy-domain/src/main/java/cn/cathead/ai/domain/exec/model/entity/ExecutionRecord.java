package cn.cathead.ai.domain.exec.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 单步执行记录：保存每一轮的分析、执行与监督摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecord implements Serializable {

    private int step;

    private String analysisResult;

    private String executionResult;

    private String supervisionResult;

    private Instant timestamp;
}



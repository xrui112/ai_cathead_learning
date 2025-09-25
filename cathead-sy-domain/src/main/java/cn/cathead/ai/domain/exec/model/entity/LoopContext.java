package cn.cathead.ai.domain.exec.model.entity;

import cn.cathead.ai.domain.exec.model.entity.ExecutionRecord;
import cn.cathead.ai.domain.exec.model.entity.Emitter;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 循环上下文，驱动状态机执行。
 */
@Data
public class LoopContext {

    private int step;
    private int maxStep;

    private String currentTask;
    private boolean completed;

    private String analysisResult;
    private String executionResult;
    private String supervisionResult;
    private String finalSummary;

    private List<ExecutionRecord> executionHistory = new ArrayList<>();

    private Map<String, Object> temp = new HashMap<>();

    private Emitter<String> emitter;
}



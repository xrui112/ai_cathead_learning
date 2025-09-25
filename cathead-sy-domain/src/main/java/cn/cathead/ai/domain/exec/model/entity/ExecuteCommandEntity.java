package cn.cathead.ai.domain.exec.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 执行入口命令实体。
 * 承载模型ID、会话与知识库上下文、初始任务及其它扩展参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCommandEntity implements Serializable {

    private String modelId;

    /** 命名空间：会话ID */
    private String sessionId;

    /** 命名空间：代理ID */
    private String agentId;

    /** 命名空间：知识ID（知识库/空间） */
    private String knowledgeId;

    /** 最大执行步数 */
    private int maxStep;

    /** 初始任务描述 */
    private String task;

    /** 额外参数（如 retrieveSize 等） */
    private Map<String, Object> extraParams;
}



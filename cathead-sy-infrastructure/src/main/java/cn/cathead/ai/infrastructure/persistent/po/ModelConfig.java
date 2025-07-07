package cn.cathead.ai.infrastructure.persistent.po;


import lombok.Data;

@Data
public class ModelConfig {
    private Long id;  // 自增主键
    private String providerName;
    private String modelId;
    private String modelName;
    private String url;
    private String key;
    private String type;

    // Chat 模型字段
    private Float temperature;
    private Float topP;
    private Integer maxTokens;
    private String stop;
    private Float frequencyPenalty;
    private Float presencePenalty;

    // Embedding 字段
    private String embeddingFormat;
    private Integer numPredict;

    // 新增：乐观锁版本字段
    private Long version;
}

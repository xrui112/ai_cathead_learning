package cn.cathead.ai.infrastructure.persistent.po;


import lombok.Data;

@Data
public class ModelConfig {
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
}

package cn.cathead.ai.domain.model.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EmbeddingModelEntity extends BaseModelEntity {
    private String embeddingFormat;

    private Integer numPredict;
    
    /**
     * 向量维度（某些模型支持设置）
     */
    private Integer dimensions;
    private Integer maxInputLength;
    private Boolean supportBatch;
    private Integer maxBatchSize;
    private Boolean normalize;
    private String similarityMetric;
    
    // 动态属性，存储模型的扩展参数
    private Map<String, Object> dynamicProperties;
}

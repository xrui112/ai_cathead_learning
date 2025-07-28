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
    
    // 动态属性，存储模型的扩展参数
    private Map<String, Object> dynamicProperties;
}

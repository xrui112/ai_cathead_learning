package cn.cathead.ai.domain.model.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EmbeddingModelEntity extends BaseModelEntity {
    private String embeddingFormat;

    private Integer numPredict;
}

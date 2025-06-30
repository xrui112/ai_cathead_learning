package cn.cathead.ai.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EmbeddingModelDTO extends BaseModelDTO {
    private String embeddingFormat;

    private Integer numPredict;
}

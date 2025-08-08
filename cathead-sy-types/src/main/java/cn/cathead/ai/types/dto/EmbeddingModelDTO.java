package cn.cathead.ai.types.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmbeddingModelDTO extends BaseModelDTO {
    private String embeddingFormat;
    private Integer numPredict;
    
    /**
     * 向量维度（某些模型支持设置）
     */
    private Integer dimensions;
}

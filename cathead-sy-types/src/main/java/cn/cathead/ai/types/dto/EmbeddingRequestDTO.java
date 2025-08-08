package cn.cathead.ai.types.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Embedding请求DTO
 * 用于文本向量化请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequestDTO implements Serializable {


    private String modelId;

    private String text;

    private List<String> texts;


    public EmbeddingRequestDTO(String modelId, String text) {
        this.modelId = modelId;
        this.text = text;
    }

    public EmbeddingRequestDTO(String modelId, List<String> texts) {
        this.modelId = modelId;
        this.texts = texts;
    }
}
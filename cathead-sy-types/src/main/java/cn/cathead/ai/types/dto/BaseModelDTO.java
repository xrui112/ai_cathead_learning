package cn.cathead.ai.types.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class BaseModelDTO {
    private String providerName;
    private String modelId;
    private String modelName;
    private String url;
    private String key;
    private String type; // "chat" / "embedding"
}

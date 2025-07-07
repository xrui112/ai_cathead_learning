package cn.cathead.ai.domain.model.model.entity;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
public abstract class BaseModelEntity implements Serializable {
    private String modelId;

    private String providerName;

    private String modelName;
    private String url;
    private String key;
    private String type; // chat / embedding

    private Long version;      // 乐观锁版本字段
}

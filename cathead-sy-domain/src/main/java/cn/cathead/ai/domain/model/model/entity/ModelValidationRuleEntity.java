package cn.cathead.ai.domain.model.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelValidationRuleEntity implements Serializable {
    private Long id;
    private String providerName;
    private String modelType; // chat/embedding
    private String fieldName;

    // 规则
    private String fieldType; // string/number/boolean/array
    private Boolean required;
    private String defaultValue;

    private Double minValue;
    private Double maxValue;
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private String enumValues; // 逗号分隔
    private String customValidator;

    private String errorMessage;
    private String fieldLabel;
    private String fieldDescription;
    private String placeholder;

    private Boolean enabled;
}



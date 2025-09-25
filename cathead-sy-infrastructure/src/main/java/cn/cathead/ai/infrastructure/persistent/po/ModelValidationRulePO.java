package cn.cathead.ai.infrastructure.persistent.po;

import lombok.Data;

@Data
public class ModelValidationRulePO {
    private Long id;
    private String providerName;
    private String modelType;
    private String fieldName;
    private String fieldType;
    private Boolean required;
    private String defaultValue;
    private Double minValue;
    private Double maxValue;
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private String enumValues;
    private String customValidator;
    private String errorMessage;
    private String fieldLabel;
    private String fieldDescription;
    private String placeholder;
    private Boolean enabled;
}



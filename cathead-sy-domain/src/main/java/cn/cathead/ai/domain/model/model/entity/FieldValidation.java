package cn.cathead.ai.domain.model.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 校验规则
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidation {
    private Integer minLength;
    private Integer maxLength;
    private Double minValue;
    private Double maxValue;
    private String pattern;        // 正则表达式
    private List<String> allowedValues; // 允许的值
    private String customValidator; // 自定义校验器名称
}
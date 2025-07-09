package cn.cathead.ai.domain.model.model.entity;

import cn.cathead.ai.domain.model.model.valobj.FieldType;
import lombok.Data;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表单字段定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {
    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段标签
     */
    private String label;

    /**
     * 字段类型
     */
    private FieldType type;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 选项列表（用于SELECT类型）
     */
    private List<String> options;

    /**
     * 校验规则
     */
    private FieldValidation validation;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 是否显示
     */
    private boolean visible = true;
}
package cn.cathead.ai.domain.model.service.DynamicForm.DynamicFormValidator;

import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
import cn.cathead.ai.domain.model.model.entity.FieldValidation;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.model.valobj.FieldType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Slf4j
public class DynamicFormValidator {

    /**
     * 校验表单数据
     */
    public ValidationResult validateFormData(FormConfiguration config, Map<String, Object> formData) {
        ValidationResult result = new ValidationResult();
        
        if (config == null) {
            result.addError("system", "表单配置不存在");
            return result;
        }

        if (formData == null) {
            result.addError("system", "表单数据不能为空");
            return result;
        }
        // 遍历所有字段定义进行校验
        for (FieldDefinition field : config.getFields()) {
            validateField(field, formData, result);
        }
        
        return result;
    }
    
    /**
     * 校验单个字段
     */
    private void validateField(FieldDefinition field, Map<String, Object> formData, ValidationResult result) {
        String fieldName = field.getName();
        Object fieldValue = formData.get(fieldName);
        // 1. 必填校验
        if (field.isRequired() && isEmpty(fieldValue)) {
            result.addError(fieldName, field.getLabel() + "是必填项");
            return; // 必填校验失败，不继续其他校验
        }
        
        // 2. 如果值为空且非必填，跳过后续校验
        if (isEmpty(fieldValue)) {
            return;
        }
        
        // 3. 类型校验
        if (!validateFieldType(field.getType(), fieldValue)) {
            result.addError(fieldName, field.getLabel() + "类型不匹配，期望类型: " + field.getType());
            return;
        }
        
        // 4. 选项校验（针对SELECT类型）
        if (field.getType() == FieldType.SELECT && field.getOptions() != null) {
            validateSelectOptions(field, fieldValue, result);
        }
        
        // 5. 验证规则校验
        if (field.getValidation() != null) {
            validateFieldRules(field, fieldValue, result);
        }
    }
    
    /**
     * 判断值是否为空
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return !StringUtils.hasText((String) value);
        }
        return false;
    }
    
    /**
     * 校验字段类型
     */
    private boolean validateFieldType(FieldType expectedType, Object value) {
        if (value == null) {
            return true; // null值在类型校验中认为是有效的
        }
        
        switch (expectedType) {
            case TEXT:
            case PASSWORD:
            case SELECT:
                return value instanceof String;
            case NUMBER:
                return value instanceof Number || isNumericString(value);
            case BOOLEAN:
                return value instanceof Boolean || isBooleanString(value);
            default:
                return true; // 未知类型默认通过
        }
    }
    
    /**
     * 判断是否为数字字符串
     */
    private boolean isNumericString(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        try {
            Double.parseDouble((String) value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 判断是否为布尔字符串
     */
    private boolean isBooleanString(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String str = ((String) value).toLowerCase();
        return "true".equals(str) || "false".equals(str);
    }
    
    /**
     * 校验选择项
     */
    private void validateSelectOptions(FieldDefinition field, Object value, ValidationResult result) {
        String stringValue = value.toString();
        if (!field.getOptions().contains(stringValue)) {
            result.addError(field.getName(), 
                field.getLabel() + "的值必须是以下选项之一: " + field.getOptions());
        }
    }
    
    /**
     * 校验字段规则
     */
    private void validateFieldRules(FieldDefinition field, Object value, ValidationResult result) {
        FieldValidation validation = field.getValidation();
        String fieldName = field.getName();
        String fieldLabel = field.getLabel();
        
        // 长度校验（针对字符串）
        if (value instanceof String) {
            String stringValue = (String) value;
            
            if (validation.getMinLength() != null && stringValue.length() < validation.getMinLength()) {
                result.addError(fieldName, fieldLabel + "长度不能少于" + validation.getMinLength() + "个字符");
            }
            
            if (validation.getMaxLength() != null && stringValue.length() > validation.getMaxLength()) {
                result.addError(fieldName, fieldLabel + "长度不能超过" + validation.getMaxLength() + "个字符");
            }
        }
        
        // 数值范围校验
        if (value instanceof Number || isNumericString(value)) {
            double numValue = value instanceof Number ? 
                ((Number) value).doubleValue() : 
                Double.parseDouble(value.toString());
            
            if (validation.getMinValue() != null && numValue < validation.getMinValue()) {
                result.addError(fieldName, fieldLabel + "不能小于" + validation.getMinValue());
            }
            
            if (validation.getMaxValue() != null && numValue > validation.getMaxValue()) {
                result.addError(fieldName, fieldLabel + "不能大于" + validation.getMaxValue());
            }
        }
        
        // 正则表达式校验
        if (validation.getPattern() != null && value instanceof String) {
            validatePattern(fieldName, fieldLabel, (String) value, validation.getPattern(), result);
        }
        
        // 允许值校验
        if (validation.getAllowedValues() != null && !validation.getAllowedValues().isEmpty()) {
            String stringValue = value.toString();
            if (!validation.getAllowedValues().contains(stringValue)) {
                result.addError(fieldName, fieldLabel + "的值必须是以下选项之一: " + validation.getAllowedValues());
            }
        }
    }
    
    /**
     * 正则表达式校验
     */
    private void validatePattern(String fieldName, String fieldLabel, String value, String pattern, ValidationResult result) {
        try {
            Pattern compiledPattern = Pattern.compile(pattern);
            if (!compiledPattern.matcher(value).matches()) {
                result.addError(fieldName, fieldLabel + "格式不正确");
            }
        } catch (Exception e) {
            log.warn("正则表达式校验失败，字段: {}, 模式: {}, 错误: {}", fieldName, pattern, e.getMessage());
            result.addError(fieldName, fieldLabel + "格式校验失败");
        }
    }
}

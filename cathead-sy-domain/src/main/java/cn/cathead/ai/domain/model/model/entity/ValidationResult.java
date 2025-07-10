package cn.cathead.ai.domain.model.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单校验结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    /**
     * 是否校验通过
     */
    private boolean valid = true;
    
    /**
     * 错误信息映射（字段名 -> 错误信息列表）
     */
    @Builder.Default
    private Map<String, List<String>> errors = new HashMap<>();
    
    /**
     * 添加错误信息
     */
    public void addError(String fieldName, String errorMessage) {
        this.valid = false;
        this.errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
    }
    
    /**
     * 获取所有错误信息
     */
    public List<String> getAllErrors() {
        List<String> allErrors = new ArrayList<>();
        for (List<String> fieldErrors : errors.values()) {
            allErrors.addAll(fieldErrors);
        }
        return allErrors;
    }
} 
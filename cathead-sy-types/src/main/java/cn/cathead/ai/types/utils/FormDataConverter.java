package cn.cathead.ai.types.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 表单数据转换工具类
 * 统一处理表单数据的类型转换逻辑
 */
@Slf4j
public class FormDataConverter {

    /**
     * 安全获取字符串值
     */
    public static String getStringValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        return value != null ? value.toString() : null;
    }



    /**
     * 安全获取Float值
     */
    public static Float getFloatValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        if (value == null) return null;
        if (value instanceof Float) return (Float) value;
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Float，key: {}, value: {}", key, value);
            return null;
        }
    }


    /**
     * 安全获取Integer值
     */
    public static Integer getIntegerValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Integer，key: {}, value: {}", key, value);
            return null;
        }
    }



    /**
     * 安全获取字符串数组值
     */
    public static String[] getStringArrayValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        if (value == null) return null;
        if (value instanceof String[]) return (String[]) value;
        if (value instanceof String) {
            String stringValue = (String) value;
            if (!StringUtils.hasText(stringValue)) return null;
            return stringValue.split(",");
        }
        return null;
    }

    /**
     * 提取动态属性
     * @param formData 表单数据
     * @param standardFields 标准字段名称列表
     * @return 动态属性映射，如果没有动态属性则返回null
     */
    public static Map<String, Object> extractDynamicProperties(Map<String, Object> formData, String... standardFields) {
        Map<String, Object> dynamicProperties = new HashMap<>();
        Set<String> standardFieldSet = Set.of(standardFields);

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            if (!standardFieldSet.contains(entry.getKey())) {
                dynamicProperties.put(entry.getKey(), entry.getValue());
            }
        }

        return dynamicProperties.isEmpty() ? null : dynamicProperties;
    }
}
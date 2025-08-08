package cn.cathead.ai.types.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class ReflectionUtils {

    /**
     * 查找setter方法
     */
    public static Method findSetterMethod(Class<?> clazz, String propertyName, Object value) {
        // 生成可能的方法名列表
        List<String> possibleNames = generatePossibleMethodNames(propertyName);
        
        for (String methodName : possibleNames) {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                    log.debug("找到匹配的方法: {} -> {}", propertyName, methodName);
                    return method;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 生成可能的方法名列表
     * 支持：直接匹配、驼峰转换
     */
    public static List<String> generatePossibleMethodNames(String propertyName) {
        List<String> names = new ArrayList<>();
        
        // 1. 直接匹配：topk
        names.add(propertyName);
        
        // 2. 驼峰转换：topk -> topK, top_k -> topK
        names.add(toCamelCase(propertyName));
        
        // 去重并保持顺序
        return names.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 转换为驼峰命名
     * topk -> topK, top_k -> topK, frequency_penalty -> frequencyPenalty
     */
    public static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        // 处理下划线分隔的情况
        if (str.contains("_")) {
            String[] parts = str.split("_");
            StringBuilder result = new StringBuilder(parts[0].toLowerCase());
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].toLowerCase();
                result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
            return result.toString();
        }
        
        // 处理常见的缩写转换
        switch (str.toLowerCase()) {
            case "topk": return "topK";
            case "topp": return "topP";
            case "maxTokens": return "maxTokens";
            case "maxtokens": return "maxTokens";
            default: return str;
        }
    }
    
    /**
     * 转换值到目标类型
     */
    public static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // 基本类型转换
        if (targetType == Double.class || targetType == double.class) {
            return ((Number) value).doubleValue();
        } else if (targetType == Float.class || targetType == float.class) {
            return ((Number) value).floatValue();
        } else if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        } else if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        } else if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value.toString());
        }
        
        return value;
    }
}
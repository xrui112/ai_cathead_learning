package cn.cathead.ai.types.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * JSON工具类，用于处理Map和JSON字符串的转换
 */
@Slf4j
public class JsonUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Map转JSON失败: {}", e.getMessage(), e);
            return null;
        }
    }

    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
} 
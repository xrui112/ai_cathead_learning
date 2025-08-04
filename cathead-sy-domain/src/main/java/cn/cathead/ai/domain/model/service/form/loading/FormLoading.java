package cn.cathead.ai.domain.model.service.form.loading;

import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
import cn.cathead.ai.domain.model.model.entity.FieldValidation;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.valobj.FieldType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FormLoading {
    
    private Map<String, FormConfiguration> configCache = new ConcurrentHashMap<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    @PostConstruct
    public void loadConfigurations() {
        try {
            log.info("开始加载动态表单配置...");
            
            // 从classpath加载YAML配置文件
            ClassPathResource resource = new ClassPathResource("form/dynamic-form.yml");
            if (!resource.exists()) {
                log.warn("动态表单配置文件不存在: form/dynamic-form.yml");
                return;
            }
            try (InputStream inputStream = resource.getInputStream()) {
                // 解析YAML配置
                Map<String, Object> yamlConfig = yamlMapper.readValue(inputStream, Map.class);

                // 获取form-configurations列表
                List<Map<String, Object>> configs = (List<Map<String, Object>>) yamlConfig.get("form-configurations");
                if (configs == null || configs.isEmpty()) {
                    log.warn("未找到表单配置项");
                    return;
                }
                
                // 解析每个配置项
                for (Map<String, Object> configMap : configs) {
                    FormConfiguration config = parseFormConfiguration(configMap);
                    if (config != null) {
                        String key = config.getProvider() + ":" + config.getType();
                        configCache.put(key, config);
                        log.info("加载表单配置: {} -> {} 个字段", key, config.getFields().size());
                    }
                }
            } catch (IOException e) {
                log.error("读取动态表单配置文件失败", e);
            }
        } catch (Exception e) {
            log.error("加载动态表单配置时发生错误", e);
        }
    }
    
    /**
     * 解析表单配置
     */
    private FormConfiguration parseFormConfiguration(Map<String, Object> configMap) {
        try {
            String provider = (String) configMap.get("provider");
            String type = (String) configMap.get("type");
            List<Map<String, Object>> fieldsList = (List<Map<String, Object>>) configMap.get("fields");
            
            if (provider == null || type == null || fieldsList == null) {
                log.warn("表单配置缺少必要字段: provider={}, type={}, fields={}", 
                        provider, type, fieldsList);
                return null;
            }
            
            // 解析字段定义
            List<FieldDefinition> fields = fieldsList.stream()
                    .map(this::parseFieldDefinition)
                    .filter(field -> field != null)
                    .collect(Collectors.toList());
            
            return FormConfiguration.builder()
                    .provider(provider)
                    .type(type)
                    .fields(fields)
                    .build();
                    
        } catch (Exception e) {
            log.error("解析表单配置失败: {}", configMap, e);
            return null;
        }
    }
    
    /**
     * 解析字段定义
     */
    private FieldDefinition parseFieldDefinition(Map<String, Object> fieldMap) {
        try {
            String name = (String) fieldMap.get("name");
            String label = (String) fieldMap.get("label");
            String typeStr = (String) fieldMap.get("type");
            
            if (name == null || label == null || typeStr == null) {
                log.warn("字段定义缺少必要属性: name={}, label={}, type={}", name, label, typeStr);
                return null;
            }
            
            FieldType type = FieldType.valueOf(typeStr);
            boolean required = Boolean.TRUE.equals(fieldMap.get("required"));
            boolean visible = !Boolean.FALSE.equals(fieldMap.get("visible")); // 默认可见
            Object defaultValue = fieldMap.get("defaultValue");
            String description = (String) fieldMap.get("description");
            List<String> options = (List<String>) fieldMap.get("options");
            
            // 解析验证规则
            FieldValidation validation = null;
            Map<String, Object> validationMap = (Map<String, Object>) fieldMap.get("validation");
            if (validationMap != null) {
                validation = parseFieldValidation(validationMap);
            }
            
            return FieldDefinition.builder()
                    .name(name)
                    .label(label)
                    .type(type)
                    .required(required)
                    .visible(visible)
                    .defaultValue(defaultValue)
                    .description(description)
                    .options(options)
                    .validation(validation)
                    .build();
                    
        } catch (Exception e) {
            log.error("解析字段定义失败: {}", fieldMap, e);
            return null;
        }
    }
    
    /**
     * 解析字段验证规则
     */
    private FieldValidation parseFieldValidation(Map<String, Object> validationMap) {
        try {
            Integer minLength = safeCastToInteger(validationMap.get("minLength"));
            Integer maxLength = safeCastToInteger(validationMap.get("maxLength"));
            Double minValue = safeCastToDouble(validationMap.get("minValue"));
            Double maxValue = safeCastToDouble(validationMap.get("maxValue"));
            String pattern = (String) validationMap.get("pattern");
            List<String> allowedValues = (List<String>) validationMap.get("allowedValues");
            String customValidator = (String) validationMap.get("customValidator");
            
            return FieldValidation.builder()
                    .minLength(minLength)
                    .maxLength(maxLength)
                    .minValue(minValue)
                    .maxValue(maxValue)
                    .pattern(pattern)
                    .allowedValues(allowedValues)
                    .customValidator(customValidator)
                    .build();
                    
        } catch (Exception e) {
            log.error("解析字段验证规则失败: {}", validationMap, e);
            return null;
        }
    }
    
    /**
     * 安全转换为Integer
     */
    private Integer safeCastToInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Integer: {}", value);
            return null;
        }
    }
    
    /**
     * 安全转换为Double
     */
    private Double safeCastToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Double: {}", value);
            return null;
        }
    }
    
    public FormConfiguration getFormConfiguration(String provider, String type) {
        String key = provider + ":" + type;
        return configCache.get(key);
    }
}
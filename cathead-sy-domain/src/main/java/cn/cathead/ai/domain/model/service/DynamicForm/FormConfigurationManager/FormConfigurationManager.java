package cn.cathead.ai.domain.model.service.DynamicForm.FormConfigurationManager;

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
public class FormConfigurationManager {
    
    private Map<String, FormConfiguration> configCache = new ConcurrentHashMap<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    @PostConstruct
    public void loadConfigurations() {
        try {
            log.info("开始加载动态表单配置...");
            
            // 从classpath加载YAML配置文件
            ClassPathResource resource = new ClassPathResource("dynamicForm/dynamic-form.yml");
            if (!resource.exists()) {
                log.warn("动态表单配置文件不存在: dynamic-form/dynamic-form.yml");
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
                /**  configMap:
                 * 1    provider: "ollama"
                 *     type: "chat"
                 *     ....
                 *
                 * 2    provider: "ollama"
                 *      type: "embedding"
                 *      ....
                 *
                 */
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
            Boolean required = (Boolean) fieldMap.get("required");
            Object defaultValue = fieldMap.get("defaultValue");
            List<String> options = (List<String>) fieldMap.get("options");
            String description = (String) fieldMap.get("description");
            Boolean visible = (Boolean) fieldMap.get("visible");
            
            if (name == null) {
                log.warn("字段定义缺少name属性: {}", fieldMap);
                return null;
            }
            
            // 解析字段类型
            FieldType fieldType = parseFieldType(typeStr);
            if (fieldType == null) {
                log.warn("未知的字段类型: {}", typeStr);
                return null;
            }
            
            // 解析验证规则
            FieldValidation validation = parseFieldValidation((Map<String, Object>) fieldMap.get("validation"));
            
            return FieldDefinition.builder()
                    .name(name)
                    .label(label != null ? label : name)
                    .type(fieldType)
                    .required(required != null ? required : false)
                    .defaultValue(defaultValue)
                    .options(options)
                    .validation(validation)
                    .description(description)
                    .visible(visible != null ? visible : true)
                    .build();
                    
        } catch (Exception e) {
            log.error("解析字段定义失败: {}", fieldMap, e);
            return null;
        }
    }
    
    /**
     * 解析字段类型
     */
    private FieldType parseFieldType(String typeStr) {
        if (typeStr == null) {
            return FieldType.TEXT; // 默认为文本类型
        }
        
        try {
            return FieldType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无法解析字段类型: {}", typeStr);
            return null;
        }
    }
    
    /**
     * 解析字段验证规则
     */
    private FieldValidation parseFieldValidation(Map<String, Object> validationMap) {
        if (validationMap == null) {
            return null;
        }
        
        try {
            return FieldValidation.builder()
                    .minLength(safeCastToInteger(validationMap.get("minLength")))
                    .maxLength(safeCastToInteger(validationMap.get("maxLength")))
                    .minValue(safeCastToDouble(validationMap.get("minValue")))
                    .maxValue(safeCastToDouble(validationMap.get("maxValue")))
                    .pattern((String) validationMap.get("pattern"))
                    .allowedValues((List<String>) validationMap.get("allowedValues"))
                    .customValidator((String) validationMap.get("customValidator"))
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
    
    public List<FieldDefinition> getVisibleFields(String provider, String type) {
        FormConfiguration config = getFormConfiguration(provider, type);
        if (config == null) {
            return List.of();
        }
        return config.getFields().stream()
                .filter(FieldDefinition::isVisible)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有已加载的配置
     */
    public Map<String, FormConfiguration> getAllConfigurations() {
        return new ConcurrentHashMap<>(configCache);
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfigurations() {
        configCache.clear();
        loadConfigurations();
    }
    
    /**
     * 检查配置是否存在
     */
    public boolean hasConfiguration(String provider, String type) {
        String key = provider + ":" + type;
        return configCache.containsKey(key);
    }
}
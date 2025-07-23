package cn.cathead.ai.domain.model.service.dynamicform.dynamicformimpl;


import cn.cathead.ai.domain.model.service.dynamicform.IDynamicForm;
import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.service.dynamicform.dynamicformvalidator.DynamicFormValidator;
import cn.cathead.ai.domain.model.service.dynamicform.formconfigurationmanager.FormConfigurationManager;
import cn.cathead.ai.domain.model.service.modelcreation.IModelCreationService;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.Arrays;
import java.util.Map;

/**
 * 动态表单实现类
 */
@Component
@Slf4j
public class DynamicFormImpl implements IDynamicForm {

    @Autowired
    private FormConfigurationManager formConfigurationManager;

    @Autowired
    private DynamicFormValidator dynamicFormValidator;

    private IModelCreationService modelCreationService;

    @Autowired
    public DynamicFormImpl(IModelCreationService modelCreationService) {
        this.modelCreationService = modelCreationService;
    }

    /**
     * 根据provider和type获取表单配置
     */
    @Override
    public FormConfiguration getFormConfiguration(String provider, String type) {
        log.info("获取表单配置，provider: {}, type: {}", provider, type);
        // 委托给配置管理器
        return formConfigurationManager.getFormConfiguration(provider, type);
    }

    /**
     * 校验表单数据
     */
    @Override
    public ValidationResult validateFormData(String provider, String type, Map<String, Object> formData) {
        log.info("校验表单数据，provider: {}, type: {}, data: {}", provider, type, formData);

        // 获取表单配置
        FormConfiguration config = getFormConfiguration(provider, type);
        if (config == null) {
            ValidationResult result = new ValidationResult();
            result.addError("system", "不支持的提供商或类型: " + provider + ":" + type);
            return result;
        }
        // 委托给校验器进行校验
        return dynamicFormValidator.validateFormData(config, formData);
    }

    /**
     * 提交表单并创建模型
     */
    @Override
    public String submitForm(String provider, String type, Map<String, Object> formData) {
        log.info("提交表单，provider: {}, type: {}, data: {}", provider, type, formData);
        
        // 获取表单配置
        FormConfiguration config = getFormConfiguration(provider, type);
        if (config == null) {
            throw new RuntimeException("不支持的提供商或类型: " + provider + ":" + type);
        }
        
        // 先校验数据（校验过程中会自动应用默认值）
        Map<String, Object> formDataWithDefaults = dynamicFormValidator.applyDefaultValues(config, formData);
        log.debug("应用默认值后的表单数据: {}", formDataWithDefaults);

        ValidationResult validationResult = dynamicFormValidator.validateFormData(config, formDataWithDefaults);
        if (!validationResult.isValid()) {
            log.error("表单数据校验失败: {}", validationResult.getAllErrors());
            throw new RuntimeException("表单数据校验失败: " + validationResult.getAllErrors());
        }

        try {
            // 根据类型创建对应的模型
            if ("chat".equalsIgnoreCase(type)) {
                ChatModelDTO chatModelDTO = buildChatModelDTO(provider, formDataWithDefaults, config);
                modelCreationService.createChatModel(chatModelDTO);
                log.info("成功创建Chat模型，provider: {}, modelName: {}", provider, chatModelDTO.getModelName());
                return "Chat模型创建成功";
            } else if ("embedding".equalsIgnoreCase(type)) {
                EmbeddingModelDTO embeddingModelDTO = buildEmbeddingModelDTO(provider, formDataWithDefaults, config);
                modelCreationService.createEmbeddingModel(embeddingModelDTO);
                log.info("成功创建Embedding模型，provider: {}, modelName: {}", provider, embeddingModelDTO.getModelName());
                return "Embedding模型创建成功";
            } else {
                throw new IllegalArgumentException("不支持的模型类型: " + type);
            }
        } catch (Exception e) {
            log.error("创建模型失败，provider: {}, type: {}, 错误: {}", provider, type, e.getMessage(), e);
            throw new RuntimeException("创建模型失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建Chat模型DTO
     */
    private ChatModelDTO buildChatModelDTO(String provider, Map<String, Object> formData, FormConfiguration config) {
        return ChatModelDTO.builder()
                .providerName(provider)
                .modelName(getStringValue(formData, "modelName", getFieldDefinition(config, "modelName")))
                .url(getStringValue(formData, "url", getFieldDefinition(config, "url")))
                .key(getStringValue(formData, "key", getFieldDefinition(config, "key")))
                .type("chat")
                .temperature(getFloatValue(formData, "temperature", getFieldDefinition(config, "temperature")))
                .topP(getFloatValue(formData, "topP", getFieldDefinition(config, "topP")))
                .maxTokens(getIntegerValue(formData, "maxTokens", getFieldDefinition(config, "maxTokens")))
                .presencePenalty(getFloatValue(formData, "presencePenalty", getFieldDefinition(config, "presencePenalty")))
                .frequencyPenalty(getFloatValue(formData, "frequencyPenalty", getFieldDefinition(config, "frequencyPenalty")))
                .stop(getStringArrayValue(formData, "stop", getFieldDefinition(config, "stop")))
                .build();
    }

    /**
     * 构建Embedding模型DTO
     */
    private EmbeddingModelDTO buildEmbeddingModelDTO(String provider, Map<String, Object> formData, FormConfiguration config) {
        return EmbeddingModelDTO.builder()
                .providerName(provider)
                .modelName(getStringValue(formData, "modelName", getFieldDefinition(config, "modelName")))
                .url(getStringValue(formData, "url", getFieldDefinition(config, "url")))
                .key(getStringValue(formData, "key", getFieldDefinition(config, "key")))
                .type("embedding")
                .embeddingFormat(getStringValue(formData, "embeddingFormat", getFieldDefinition(config, "embeddingFormat")))
                .numPredict(getIntegerValue(formData, "numPredict", getFieldDefinition(config, "numPredict")))
                .build();
    }
    
    /**
     * 根据字段名称获取字段定义
     */
    private FieldDefinition getFieldDefinition(FormConfiguration config, String fieldName) {
        if (config == null || config.getFields() == null) {
            return null;
        }
        return config.getFields().stream()
                .filter(field -> fieldName.equals(field.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 安全获取字符串值，支持YAML默认值
     */
    private String getStringValue(Map<String, Object> formData, String key, FieldDefinition field) {
        Object value = formData.get(key);
        if (value == null && field != null && field.getDefaultValue() != null) {
            log.debug("使用YAML默认值: {} = {}", key, field.getDefaultValue());
            return field.getDefaultValue().toString();
        }
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 安全获取Float值，支持YAML默认值
     */
    private Float getFloatValue(Map<String, Object> formData, String key, FieldDefinition field) {
        Object value = formData.get(key);
        if (value == null && field != null && field.getDefaultValue() != null) {
            try {
                Float defaultValue = Float.valueOf(field.getDefaultValue().toString());
                log.debug("使用YAML默认值: {} = {}", key, defaultValue);
                return defaultValue;
            } catch (NumberFormatException e) {
                log.warn("YAML默认值格式错误，key: {}, defaultValue: {}", key, field.getDefaultValue());
            }
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将值转换为Float，key: {}, value: {}", key, value);
            return null;
        }
    }

    /**
     * 安全获取Integer值，支持YAML默认值
     */
    private Integer getIntegerValue(Map<String, Object> formData, String key, FieldDefinition field) {
        Object value = formData.get(key);
        if (value == null && field != null && field.getDefaultValue() != null) {
            try {
                Integer defaultValue = Integer.valueOf(field.getDefaultValue().toString());
                log.debug("使用YAML默认值: {} = {}", key, defaultValue);
                return defaultValue;
            } catch (NumberFormatException e) {
                log.warn("YAML默认值格式错误，key: {}, defaultValue: {}", key, field.getDefaultValue());
            }
        }
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
            log.warn("无法将值转换为Integer，key: {}, value: {}", key, value);
            return null;
        }
    }

    /**
     * 安全获取字符串数组值，支持YAML默认值
     */
    private String[] getStringArrayValue(Map<String, Object> formData, String key, FieldDefinition field) {
        Object value = formData.get(key);
        if (value == null && field != null && field.getDefaultValue() != null) {
            try {
                String defaultValue = field.getDefaultValue().toString();
                if (StringUtils.hasText(defaultValue)) {
                    String[] result = defaultValue.split(",");
                    log.debug("使用YAML默认值: {} = {}", key, Arrays.toString(result));
                    return result;
                }
            } catch (Exception e) {
                log.warn("YAML默认值格式错误，key: {}, defaultValue: {}", key, field.getDefaultValue());
            }
        }
        if (value == null) {
            return null;
        }
        if (value instanceof String[]) {
            return (String[]) value;
        }
        if (value instanceof String) {
            // 如果是字符串，按逗号分割
            String stringValue = (String) value;
            if (!StringUtils.hasText(stringValue)) {
                return null;
            }
            return stringValue.split(",");
        }
        log.warn("无法将值转换为String[]，key: {}, value: {}", key, value);
        return null;
    }
}

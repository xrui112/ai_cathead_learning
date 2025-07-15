package cn.cathead.ai.domain.model.service.DynamicForm.DynamicFormImpl;

import cn.cathead.ai.api.dto.ChatModelDTO;
import cn.cathead.ai.api.dto.EmbeddingModelDTO;
import cn.cathead.ai.domain.model.service.DynamicForm.IDynamicForm;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
import cn.cathead.ai.domain.model.model.entity.FieldValidation;
import cn.cathead.ai.domain.model.model.valobj.FieldType;
import cn.cathead.ai.domain.model.service.DynamicForm.DynamicFormValidator.DynamicFormValidator;
import cn.cathead.ai.domain.model.service.DynamicForm.FormConfigurationManager.FormConfigurationManager;
import cn.cathead.ai.domain.model.service.IModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

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

    private IModelService modelService;

    @Autowired
    public DynamicFormImpl(IModelService modelService) {
        this.modelService = modelService;
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
        // 先校验数据
        ValidationResult validationResult = validateFormData(provider, type, formData);
        if (!validationResult.isValid()) {
            log.error("表单数据校验失败: {}", validationResult.getAllErrors());
            throw new RuntimeException("表单数据校验失败: " + validationResult.getAllErrors());
        }
        try {
            // 根据类型创建对应的模型
            if ("chat".equalsIgnoreCase(type)) {
                ChatModelDTO chatModelDTO = buildChatModelDTO(provider, formData);
                modelService.creatModel(chatModelDTO);
                log.info("成功创建Chat模型，provider: {}, modelName: {}", provider, chatModelDTO.getModelName());
                return "Chat模型创建成功";
            } else if ("embedding".equalsIgnoreCase(type)) {
                EmbeddingModelDTO embeddingModelDTO = buildEmbeddingModelDTO(provider, formData);
                modelService.creatModel(embeddingModelDTO);
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
    private ChatModelDTO buildChatModelDTO(String provider, Map<String, Object> formData) {
        return ChatModelDTO.builder()
                .providerName(provider)
                .modelName(getStringValue(formData, "modelName"))
                .url(getStringValue(formData, "url"))
                .key(getStringValue(formData, "key"))
                .type("chat")
                .temperature(getFloatValue(formData, "temperature"))
                .topP(getFloatValue(formData, "topP"))
                .maxTokens(getIntegerValue(formData, "maxTokens"))
                .presencePenalty(getFloatValue(formData, "presencePenalty"))
                .frequencyPenalty(getFloatValue(formData, "frequencyPenalty"))
                .stop(getStringArrayValue(formData, "stop"))
                .build();
    }

    /**
     * 构建Embedding模型DTO
     */
    private EmbeddingModelDTO buildEmbeddingModelDTO(String provider, Map<String, Object> formData) {
        return EmbeddingModelDTO.builder()
                .providerName(provider)
                .modelName(getStringValue(formData, "modelName"))
                .url(getStringValue(formData, "url"))
                .key(getStringValue(formData, "key"))
                .type("embedding")
                .embeddingFormat(getStringValue(formData, "embeddingFormat"))
                .numPredict(getIntegerValue(formData, "numPredict"))
                .build();
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 安全获取Float值
     */
    private Float getFloatValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
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
     * 安全获取Integer值
     */
    private Integer getIntegerValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
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
     * 安全获取字符串数组值
     */
    private String[] getStringArrayValue(Map<String, Object> formData, String key) {
        Object value = formData.get(key);
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

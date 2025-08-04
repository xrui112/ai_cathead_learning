package cn.cathead.ai.test.service;

import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
import cn.cathead.ai.domain.model.model.entity.FieldValidation;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.model.valobj.FieldType;
import cn.cathead.ai.domain.model.service.form.validator.FormValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态表单功能测试
 * 简单的单元测试，不使用Spring容器
 */
@DisplayName("动态表单功能测试")
public class DynamicFormTest {

    private FormValidator formValidator;
    private FormConfiguration testChatFormConfig;
    private Map<String, Object> validChatFormData;
    private Map<String, Object> invalidFormData;

    @BeforeEach
    public void setUp() {
        // 创建真实的校验器实例
        formValidator = new FormValidator();
        
        setupTestData();
    }

    private void setupTestData() {
        // 设置Chat模型表单配置
        List<FieldDefinition> fields = new ArrayList<>();
        
        // modelName字段
        FieldDefinition modelNameField = new FieldDefinition();
        modelNameField.setName("modelName");
        modelNameField.setLabel("模型名称");
        modelNameField.setType(FieldType.TEXT);
        modelNameField.setRequired(true);
        
        FieldValidation modelNameValidation = new FieldValidation();
        modelNameValidation.setMinLength(1);
        modelNameValidation.setMaxLength(50);
        modelNameField.setValidation(modelNameValidation);
        
        // temperature字段
        FieldDefinition temperatureField = new FieldDefinition();
        temperatureField.setName("temperature");
        temperatureField.setLabel("温度");
        temperatureField.setType(FieldType.NUMBER);
        temperatureField.setRequired(false);
        temperatureField.setDefaultValue(0.7);
        
        FieldValidation temperatureValidation = new FieldValidation();
        temperatureValidation.setMinValue(0.0);
        temperatureValidation.setMaxValue(2.0);
        temperatureField.setValidation(temperatureValidation);
        
        // url字段
        FieldDefinition urlField = new FieldDefinition();
        urlField.setName("url");
        urlField.setLabel("服务地址");
        urlField.setType(FieldType.TEXT);
        urlField.setRequired(true);
        
        FieldValidation urlValidation = new FieldValidation();
        urlValidation.setPattern("^https?://.*");
        urlField.setValidation(urlValidation);
        
        fields.add(modelNameField);
        fields.add(temperatureField);
        fields.add(urlField);
        
        testChatFormConfig = new FormConfiguration();
        testChatFormConfig.setProvider("ollama");
        testChatFormConfig.setType("chat");
        testChatFormConfig.setFields(fields);

        // 设置有效的表单数据
        validChatFormData = new HashMap<>();
        validChatFormData.put("modelName", "qwen3");
        validChatFormData.put("temperature", 0.8);
        validChatFormData.put("url", "http://localhost:11434");

        // 设置无效的表单数据
        invalidFormData = new HashMap<>();
        invalidFormData.put("modelName", ""); // 空字符串
        invalidFormData.put("temperature", 3.0); // 超出范围
        invalidFormData.put("url", "invalid-url"); // 无效URL
    }

    @Test
    @DisplayName("测试表单配置结构")
    public void testFormConfiguration() {
        // When & Then
        assertNotNull(testChatFormConfig);
        assertEquals("ollama", testChatFormConfig.getProvider());
        assertEquals("chat", testChatFormConfig.getType());
        assertEquals(3, testChatFormConfig.getFields().size());
        
        // 验证modelName字段配置
        FieldDefinition modelNameField = testChatFormConfig.getFields().stream()
                .filter(field -> "modelName".equals(field.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(modelNameField);
        assertTrue(modelNameField.isRequired());
        assertEquals(FieldType.TEXT, modelNameField.getType());
        
        // 验证temperature字段配置
        FieldDefinition temperatureField = testChatFormConfig.getFields().stream()
                .filter(field -> "temperature".equals(field.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(temperatureField);
        assertFalse(temperatureField.isRequired());
        assertEquals(FieldType.NUMBER, temperatureField.getType());
        assertEquals(0.7, temperatureField.getDefaultValue());
    }

    @Test
    @DisplayName("测试默认值应用")
    public void testApplyDefaultValues() {
        // Given
        Map<String, Object> partialData = new HashMap<>();
        partialData.put("modelName", "test-model");
        partialData.put("url", "http://localhost:11434");
        // 不提供 temperature，应该应用默认值

        // When
        Map<String, Object> result = formValidator.applyDefaultValues(testChatFormConfig, partialData);

        // Then
        assertNotNull(result);
        assertEquals("test-model", result.get("modelName"));
        assertEquals("http://localhost:11434", result.get("url"));
        assertEquals(0.7, result.get("temperature")); // 应用了默认值
    }

    @Test
    @DisplayName("测试有效数据校验")
    public void testValidateValidData() {
        // When
        ValidationResult result = formValidator.validateFormData(testChatFormConfig, validChatFormData);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("测试无效数据校验")
    public void testValidateInvalidData() {
        // When
        ValidationResult result = formValidator.validateFormData(testChatFormConfig, invalidFormData);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        
        // 检查具体的错误信息
        assertTrue(result.getErrors().containsKey("modelName"));
        assertTrue(result.getErrors().containsKey("temperature"));
        assertTrue(result.getErrors().containsKey("url"));
    }

    @Test
    @DisplayName("测试必填字段校验")
    public void testRequiredFieldValidation() {
        // Given
        Map<String, Object> missingRequiredData = new HashMap<>();
        missingRequiredData.put("temperature", 0.8);
        // 缺少必填的 modelName 和 url

        // When
        ValidationResult result = formValidator.validateFormData(testChatFormConfig, missingRequiredData);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("modelName"));
        assertTrue(result.getErrors().containsKey("url"));
    }

    @Test
    @DisplayName("测试数值范围校验")
    public void testNumericRangeValidation() {
        // Given
        Map<String, Object> outOfRangeData = new HashMap<>();
        outOfRangeData.put("modelName", "test-model");
        outOfRangeData.put("url", "http://localhost:11434");
        outOfRangeData.put("temperature", 5.0); // 超出范围 (0-2)

        // When
        ValidationResult result = formValidator.validateFormData(testChatFormConfig, outOfRangeData);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("temperature"));
    }

    @Test
    @DisplayName("测试字符串长度校验")
    public void testStringLengthValidation() {
        // Given
        Map<String, Object> wrongLengthData = new HashMap<>();
        wrongLengthData.put("modelName", "a".repeat(100)); // 超过最大长度50
        wrongLengthData.put("url", "http://localhost:11434");
        wrongLengthData.put("temperature", 0.8);

        // When
        ValidationResult result = formValidator.validateFormData(testChatFormConfig, wrongLengthData);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("modelName"));
    }

    @Test
    @DisplayName("测试正则表达式校验")
    public void testPatternValidation() {
        // Given
        Map<String, Object> invalidPatternData = new HashMap<>();
        invalidPatternData.put("modelName", "test-model");
        invalidPatternData.put("url", "invalid-url-format"); // 不符合URL格式
        invalidPatternData.put("temperature", 0.8);

        // When
        ValidationResult result = formValidator.validateFormData(testChatFormConfig, invalidPatternData);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("url"));
    }

//    @Test
//    @DisplayName("测试带默认值的完整校验流程")
//    public void testCompleteValidationWithDefaults() {
//        // Given
//        Map<String, Object> partialValidData = new HashMap<>();
//        partialValidData.put("modelName", "test-model");
//        partialValidData.put("url", "http://localhost:11434");
//        // 不提供temperature字段，让默认值生效
//
//        // When
//        Map<String, Object> dataWithDefaults = dynamicFormValidator.applyDefaultValues(testChatFormConfig, partialValidData);
//        ValidationResult result = dynamicFormValidator.validateFormData(testChatFormConfig, dataWithDefaults);
//
//        // Then
//        assertNotNull(dataWithDefaults);
//        assertEquals("test-model", dataWithDefaults.get("modelName"));
//        assertEquals("http://localhost:11434", dataWithDefaults.get("url"));
//        assertEquals(0.7, dataWithDefaults.get("temperature")); // 默认值
//
//        assertNotNull(result);
//        assertTrue(result.isValid());
//        assertTrue(result.getErrors().isEmpty());
//    }

    @Test
    @DisplayName("测试空配置处理")
    public void testNullConfigValidation() {
        // When
        ValidationResult result = formValidator.validateFormData(null, validChatFormData);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("system"));
    }

    @Test
    @DisplayName("测试空数据处理")
    public void testNullDataValidation() {
        // When
        ValidationResult result = formValidator.validateFormData(testChatFormConfig, null);

        // Then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().containsKey("system"));
    }
}
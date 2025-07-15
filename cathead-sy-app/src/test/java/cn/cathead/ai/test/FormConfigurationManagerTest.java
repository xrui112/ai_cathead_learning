package cn.cathead.ai.test;

import cn.cathead.ai.domain.model.model.entity.FieldDefinition;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.valobj.FieldType;
import cn.cathead.ai.domain.model.service.DynamicForm.FormConfigurationManager.FormConfigurationManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FormConfigurationManagerTest {

    @Autowired
    private FormConfigurationManager formConfigurationManager;

    @Test
    void testLoadConfigurations() {
        // 验证配置是否成功加载
        Map<String, FormConfiguration> allConfigs = formConfigurationManager.getAllConfigurations();
        assertNotNull(allConfigs);
        assertFalse(allConfigs.isEmpty());
        
        System.out.println("加载的配置数量: " + allConfigs.size());
        allConfigs.forEach((key, config) -> {
            System.out.println("配置: " + key + " -> " + config.getFields().size() + " 个字段");
        });
    }

    @Test
    void testGetFormConfiguration() {
        // 测试获取 Ollama Chat 配置
        FormConfiguration ollamaChatConfig = formConfigurationManager.getFormConfiguration("ollama", "chat");
        assertNotNull(ollamaChatConfig);
        assertEquals("ollama", ollamaChatConfig.getProvider());
        assertEquals("chat", ollamaChatConfig.getType());
        
        // 验证字段数量
        assertFalse(ollamaChatConfig.getFields().isEmpty());
        
        // 验证关键字段
        boolean hasModelName = ollamaChatConfig.getFields().stream()
                .anyMatch(field -> "modelName".equals(field.getName()));
        assertTrue(hasModelName, "应该包含 modelName 字段");
        
        // 验证字段类型
        FieldDefinition modelNameField = ollamaChatConfig.getFields().stream()
                .filter(field -> "modelName".equals(field.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(modelNameField);
        assertEquals(FieldType.TEXT, modelNameField.getType());
        assertTrue(modelNameField.isRequired());
    }

    @Test
    void testGetVisibleFields() {
        // 测试获取可见字段
        List<FieldDefinition> visibleFields = formConfigurationManager.getVisibleFields("ollama", "chat");
        assertNotNull(visibleFields);
        assertFalse(visibleFields.isEmpty());
        
        // 验证所有字段都是可见的
        visibleFields.forEach(field -> {
            assertTrue(field.isVisible(), "字段 " + field.getName() + " 应该是可见的");
        });
        
        System.out.println("Ollama Chat 可见字段数量: " + visibleFields.size());
        visibleFields.forEach(field -> {
            System.out.println("字段: " + field.getName() + " (" + field.getType() + ") - " + field.getLabel());
        });
    }

    @Test
    void testHasConfiguration() {
        // 测试配置存在性检查
        assertTrue(formConfigurationManager.hasConfiguration("ollama", "chat"));
        assertTrue(formConfigurationManager.hasConfiguration("ollama", "embedding"));
        assertFalse(formConfigurationManager.hasConfiguration("unknown", "chat"));
    }

    @Test
    void testFieldValidation() {
        // 测试字段验证规则
        FormConfiguration ollamaChatConfig = formConfigurationManager.getFormConfiguration("ollama", "chat");
        
        // 查找有验证规则的字段
        FieldDefinition temperatureField = ollamaChatConfig.getFields().stream()
                .filter(field -> "temperature".equals(field.getName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(temperatureField);
        assertNotNull(temperatureField.getValidation());
        assertEquals(0.0, temperatureField.getValidation().getMinValue());
        assertEquals(2.0, temperatureField.getValidation().getMaxValue());
        assertEquals(0.7, temperatureField.getDefaultValue());
    }

    @Test
    void testFieldOptions() {
        // 测试字段选项（SELECT类型）
        FormConfiguration ollamaEmbeddingConfig = formConfigurationManager.getFormConfiguration("ollama", "embedding");
        
        FieldDefinition formatField = ollamaEmbeddingConfig.getFields().stream()
                .filter(field -> "embeddingFormat".equals(field.getName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(formatField);
        assertEquals(FieldType.SELECT, formatField.getType());
        assertNotNull(formatField.getOptions());
        assertTrue(formatField.getOptions().contains("float"));
        assertTrue(formatField.getOptions().contains("base64"));
    }

    @Test
    void testReloadConfigurations() {
        // 测试重新加载配置
        int originalSize = formConfigurationManager.getAllConfigurations().size();
        
        formConfigurationManager.reloadConfigurations();
        
        int reloadedSize = formConfigurationManager.getAllConfigurations().size();
        assertEquals(originalSize, reloadedSize, "重新加载后配置数量应该相同");
    }
}
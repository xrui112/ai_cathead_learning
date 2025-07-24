package cn.cathead.ai.test.service;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.valobj.ModelPropertyVo;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.modelbean.IModelBeanManager;
import cn.cathead.ai.domain.model.service.modelcreation.modelcreationserviceimpl.ModelCreationService;
import cn.cathead.ai.types.dto.ChatModelDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 默认值应用顺序测试
 * 验证ModelCreationService中默认值应用的流程
 */
@DisplayName("默认值应用顺序测试")
public class DefaultValueOrderTest {

    @Mock
    private IModelRepository modelRepository;

    @Mock
    private IModelBeanManager modelBeanManager;

    @Mock
    private ChatModel mockChatModel;

    private ModelCreationService modelCreationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        modelCreationService = new ModelCreationService();
        // 使用反射注入依赖
        setField(modelCreationService, "iModelRepository", modelRepository);
        setField(modelCreationService, "modelBeanManager", modelBeanManager);
    }

    @Test
    @DisplayName("测试YAML默认值传递到Provider层")
    public void testYamlDefaultValuePassToProvider() {
        // Given - 模拟已经应用了YAML默认值的DTO
        ChatModelDTO chatModelDTOWithYamlDefaults = ChatModelDTO.builder()
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)    // YAML默认值
                .topP(0.9f)           // YAML默认值 (不同于ModelPropertyVo的1.0)
                .maxTokens(2048)      // YAML默认值 (不同于ModelPropertyVo的1024)
                // 这些字段为null，需要在Provider层应用ModelPropertyVo默认值
                .presencePenalty(null)
                .frequencyPenalty(null)
                .stop(null)
                .build();

        // Mock Provider层的行为
        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenAnswer(invocation -> {
                    ChatModelEntity entity = invocation.getArgument(0);
                    
                    // 验证Entity在传入Provider之前的状态
                    assertEquals(0.7f, entity.getTemperature()); // YAML默认值已应用
                    assertEquals(0.9f, entity.getTopP());         // YAML默认值已应用  
                    assertEquals(2048, entity.getMaxTokens());    // YAML默认值已应用
                    assertNull(entity.getPresencePenalty());      // 应该为null，等待Provider层处理
                    assertNull(entity.getFrequencyPenalty());     // 应该为null，等待Provider层处理
                    assertNull(entity.getStop());                 // 应该为null，等待Provider层处理
                    
                    return mockChatModel;
                });

        when(modelRepository.saveModelRecord(any(ChatModelEntity.class)))
                .thenReturn(1L);

        doNothing().when(modelBeanManager).saveChatModelToCache(any(), any());

        // When
        modelCreationService.createChatModel(chatModelDTOWithYamlDefaults);

        // Then
        ArgumentCaptor<ChatModelEntity> entityCaptor = ArgumentCaptor.forClass(ChatModelEntity.class);
        verify(modelBeanManager, times(1)).createChatModelInstance(entityCaptor.capture());

        ChatModelEntity capturedEntity = entityCaptor.getValue();
        
        // 验证YAML默认值已应用
        assertEquals(0.7f, capturedEntity.getTemperature(), "Temperature应该使用YAML默认值0.7");
        assertEquals(0.9f, capturedEntity.getTopP(), "TopP应该使用YAML默认值0.9");
        assertEquals(2048, capturedEntity.getMaxTokens(), "MaxTokens应该使用YAML默认值2048");
        
        // 验证未设置的字段为null，等待Provider层应用ModelPropertyVo默认值
        assertNull(capturedEntity.getPresencePenalty(), "PresencePenalty应该为null，等待Provider层处理");
        assertNull(capturedEntity.getFrequencyPenalty(), "FrequencyPenalty应该为null，等待Provider层处理");
        assertNull(capturedEntity.getStop(), "Stop应该为null，等待Provider层处理");
    }

    @Test
    @DisplayName("验证ModelPropertyVo默认值的正确性")
    public void testModelPropertyVoDefaultValues() {
        // 验证ModelPropertyVo中定义的默认值
        assertEquals("0.7", ModelPropertyVo.TEMPERATURE.getDefaultValue());
        assertEquals("1.0", ModelPropertyVo.TOP_K.getDefaultValue());
        assertEquals("1024", ModelPropertyVo.MAX_TOKENS.getDefaultValue());
        assertEquals("", ModelPropertyVo.STOP.getDefaultValue());
        assertEquals("0.0", ModelPropertyVo.FREQUENCY_PENALTY.getDefaultValue());
        assertEquals("0.0", ModelPropertyVo.PRESENCE_PENALTY.getDefaultValue());
        assertEquals("float", ModelPropertyVo.EMBEDDIDNGFORMAT.getDefaultValue());
        assertEquals("256", ModelPropertyVo.NUMPREDICT.getDefaultValue());
    }

    @Test
    @DisplayName("测试YAML默认值与ModelPropertyVo默认值的差异")
    public void testYamlVsModelPropertyVoDefaults() {
        // 验证YAML默认值与ModelPropertyVo默认值的差异
        // 这证明了YAML配置可以覆盖ModelPropertyVo的默认值，实现更精细化的配置
        
        // ModelPropertyVo中的默认值
        assertEquals("0.7", ModelPropertyVo.TEMPERATURE.getDefaultValue());   // 相同
        assertEquals("1.0", ModelPropertyVo.TOP_K.getDefaultValue());         // YAML是0.9，不同！
        assertEquals("1024", ModelPropertyVo.MAX_TOKENS.getDefaultValue());   // YAML是2048，不同！
        assertEquals("0.0", ModelPropertyVo.FREQUENCY_PENALTY.getDefaultValue());
        assertEquals("0.0", ModelPropertyVo.PRESENCE_PENALTY.getDefaultValue());
        
        // 这说明：
        // 1. YAML可以提供与ModelPropertyVo不同的默认值
        // 2. YAML默认值具有更高的优先级
        // 3. 未在YAML中配置的字段会在Provider层使用ModelPropertyVo默认值
    }

    @Test
    @DisplayName("测试空值字段的处理")
    public void testNullFieldHandling() {
        // Given - 部分字段为null的DTO
        ChatModelDTO chatModelDTOWithNulls = ChatModelDTO.builder()
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)        // 有值
                .topP(null)               // null，等待Provider层处理
                .maxTokens(null)          // null，等待Provider层处理
                .presencePenalty(null)    // null，等待Provider层处理
                .frequencyPenalty(null)   // null，等待Provider层处理
                .stop(null)               // null，等待Provider层处理
                .build();

        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);
        when(modelRepository.saveModelRecord(any(ChatModelEntity.class)))
                .thenReturn(1L);
        doNothing().when(modelBeanManager).saveChatModelToCache(any(), any());

        // When
        modelCreationService.createChatModel(chatModelDTOWithNulls);

        // Then
        ArgumentCaptor<ChatModelEntity> entityCaptor = ArgumentCaptor.forClass(ChatModelEntity.class);
        verify(modelBeanManager, times(1)).createChatModelInstance(entityCaptor.capture());

        ChatModelEntity entity = entityCaptor.getValue();
        
        // 验证：
        assertEquals(0.7f, entity.getTemperature());    // 有值的字段保持不变
        assertNull(entity.getTopP());                   // null字段在Provider层会被处理
        assertNull(entity.getMaxTokens());              // null字段在Provider层会被处理
        assertNull(entity.getPresencePenalty());        // null字段在Provider层会被处理
        assertNull(entity.getFrequencyPenalty());       // null字段在Provider层会被处理
        assertNull(entity.getStop());                   // null字段在Provider层会被处理
    }

    /**
     * 使用反射设置字段值
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("设置字段失败: " + fieldName, e);
        }
    }
} 
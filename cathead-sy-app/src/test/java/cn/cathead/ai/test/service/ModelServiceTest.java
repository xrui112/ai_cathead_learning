package cn.cathead.ai.test.service;

import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.IModelService;
import cn.cathead.ai.domain.model.service.ModelService;
import cn.cathead.ai.domain.model.service.dynamicform.IDynamicForm;
import cn.cathead.ai.domain.model.service.modelbean.IModelBeanManager;
import cn.cathead.ai.domain.model.service.modelcreation.IModelCreationService;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import cn.cathead.ai.types.exception.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import reactor.core.publisher.Flux;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ModelService 业务逻辑测试
 */
@DisplayName("ModelService 测试")
public class ModelServiceTest {

    @Mock
    private IModelRepository modelRepository;

    @Mock
    private IModelBeanManager modelBeanManager;

    @Mock
    private IDynamicForm dynamicForm;

    @Mock
    private IModelCreationService modelCreationService;

    @Mock
    private ChatModel mockChatModel;

    @Mock
    private EmbeddingModel mockEmbeddingModel;

    @InjectMocks
    private ModelService modelService;

    private String testModelId;
    private ChatModelDTO testChatModelDTO;
    private EmbeddingModelDTO testEmbeddingModelDTO;
    private ChatModelEntity testChatModelEntity;
    private EmbeddingModelEntity testEmbeddingModelEntity;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        setupTestData();
    }

    private void setupTestData() {
        testModelId = UUID.randomUUID().toString();
        
        // 构建测试用的DTO
        testChatModelDTO = ChatModelDTO.builder()
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)
                .topP(0.9f)
                .maxTokens(2048)
                .build();

        testEmbeddingModelDTO = EmbeddingModelDTO.builder()
                .providerName("ollama")
                .modelName("nomic-embed-text")
                .url("http://localhost:11434")
                .key("")
                .type("embedding")
                .embeddingFormat("json")
                .numPredict(512)
                .build();

        // 构建测试用的Entity
        testChatModelEntity = ChatModelEntity.builder()
                .modelId(testModelId)
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)
                .topP(0.9f)
                .maxTokens(2048)
                .version(1L)
                .build();

        testEmbeddingModelEntity = EmbeddingModelEntity.builder()
                .modelId(testModelId)
                .providerName("ollama")
                .modelName("nomic-embed-text")
                .url("http://localhost:11434")
                .key("")
                .type("embedding")
                .embeddingFormat("json")
                .numPredict(512)
                .version(1L)
                .build();
    }

    @Test
    @DisplayName("创建Chat模型")
    public void testCreateChatModel() {
        // Given
        doNothing().when(modelCreationService).createChatModel(any(ChatModelDTO.class));

        // When
        modelService.createModel(testChatModelDTO);

        // Then
        verify(modelCreationService, times(1)).createChatModel(testChatModelDTO);
    }

    @Test
    @DisplayName("创建Embedding模型")
    public void testCreateEmbeddingModel() {
        // Given
        doNothing().when(modelCreationService).createEmbeddingModel(any(EmbeddingModelDTO.class));

        // When
        modelService.createModel(testEmbeddingModelDTO);

        // Then
        verify(modelCreationService, times(1)).createEmbeddingModel(testEmbeddingModelDTO);
    }

    @Test
    @DisplayName("聊天对话测试")
    public void testChatWith() {
        // Given
        String prompt = "你好，请介绍一下自己";
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModelId(testModelId);
        chatRequestDTO.setPrompt(prompt);

        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        when(modelBeanManager.getCachedModelVersion(testModelId)).thenReturn(1L);
        when(modelBeanManager.getChatModelBean(testModelId)).thenReturn(mockChatModel);
        when(mockChatModel.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(Flux.just(mock(ChatResponse.class)));

        // When
        Flux<ChatResponse> result = modelService.chatWith(chatRequestDTO);

        // Then
        assertNotNull(result);
        // 验证Flux不为空且可以订阅
        result.blockFirst(); // 简单验证流可以消费
        
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(modelBeanManager, times(1)).getCachedModelVersion(testModelId);
        verify(modelBeanManager, times(1)).getChatModelBean(testModelId);
    }

    @Test
    @DisplayName("聊天对话失败 - 模型不存在")
    public void testChatWithModelNotFound() {
        // Given
        String prompt = "你好";
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModelId("nonexistent-model");
        chatRequestDTO.setPrompt(prompt);

        when(modelRepository.queryModelById("nonexistent-model")).thenReturn(null);
        doNothing().when(modelBeanManager).removeChatModelBean("nonexistent-model");

        // When
        Flux<ChatResponse> result = modelService.chatWith(chatRequestDTO);

        // Then
        assertNotNull(result);
        // 验证返回的是空流
        assertEquals(0, result.collectList().block().size());
        
        verify(modelRepository, times(1)).queryModelById("nonexistent-model");
        verify(modelBeanManager, times(1)).removeChatModelBean("nonexistent-model");
    }

    @Test
    @DisplayName("更新Chat模型配置成功")
    public void testUpdateChatModelConfigSuccess() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        doNothing().when(modelRepository).updateModelRecord(any(ChatModelEntity.class));
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        when(modelBeanManager.updateChatModelBean(eq(testModelId), any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);

        // When & Then
        assertDoesNotThrow(() -> modelService.updateChatModelConfig(testModelId, testChatModelDTO));
        
        verify(modelRepository, times(2)).queryModelById(testModelId);
        verify(modelRepository, times(1)).updateModelRecord(any(ChatModelEntity.class));
        verify(modelBeanManager, times(1)).updateChatModelBean(eq(testModelId), any(ChatModelEntity.class));
    }

    @Test
    @DisplayName("更新Chat模型配置失败 - 乐观锁冲突")
    public void testUpdateChatModelConfigOptimisticLockException() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
                .when(modelRepository).updateModelRecord(any(ChatModelEntity.class));

        // When & Then
        OptimisticLockException exception = assertThrows(OptimisticLockException.class, 
                () -> modelService.updateChatModelConfig(testModelId, testChatModelDTO));
        
        assertEquals("模型配置更新失败，数据已被其他用户修改", exception.getMessage());
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(modelRepository, times(1)).updateModelRecord(any(ChatModelEntity.class));
        verify(modelBeanManager, never()).updateChatModelBean(any(), any());
    }

    @Test
    @DisplayName("更新Embedding模型配置成功")
    public void testUpdateEmbeddingModelConfigSuccess() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testEmbeddingModelEntity);
        doNothing().when(modelRepository).updateModelRecord(any(EmbeddingModelEntity.class));
        when(modelRepository.queryModelById(testModelId)).thenReturn(testEmbeddingModelEntity);
        when(modelBeanManager.updateEmbeddingModelBean(eq(testModelId), any(EmbeddingModelEntity.class)))
                .thenReturn(mockEmbeddingModel);

        // When & Then
        assertDoesNotThrow(() -> modelService.updateEmbeddingModelConfig(testModelId, testEmbeddingModelDTO));
        
        verify(modelRepository, times(2)).queryModelById(testModelId);
        verify(modelRepository, times(1)).updateModelRecord(any(EmbeddingModelEntity.class));
        verify(modelBeanManager, times(1)).updateEmbeddingModelBean(eq(testModelId), any(EmbeddingModelEntity.class));
    }

    @Test
    @DisplayName("删除模型成功")
    public void testDeleteModel() {
        // Given
        doNothing().when(modelBeanManager).removeChatModelBean(testModelId);
        doNothing().when(modelBeanManager).removeEmbeddingModelBean(testModelId);
        doNothing().when(modelRepository).deleteModelRecord(testModelId);

        // When
        modelService.deleteModel(testModelId);

        // Then
        verify(modelBeanManager, times(1)).removeChatModelBean(testModelId);
        verify(modelBeanManager, times(1)).removeEmbeddingModelBean(testModelId);
        verify(modelRepository, times(1)).deleteModelRecord(testModelId);
    }

    @Test
    @DisplayName("根据ID获取模型")
    public void testGetModelById() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);

        // When
        BaseModelEntity result = modelService.getModelById(testModelId);

        // Then
        assertNotNull(result);
        assertEquals(testModelId, result.getModelId());
        assertEquals("ollama", result.getProviderName());
        verify(modelRepository, times(1)).queryModelById(testModelId);
    }

    @Test
    @DisplayName("刷新模型缓存 - Chat模型")
    public void testRefreshChatModelCache() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        when(modelBeanManager.getCachedModelVersion(testModelId)).thenReturn(0L);
        when(modelBeanManager.updateChatModelBean(eq(testModelId), any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);

        // When
        modelService.refreshModelCache(testModelId);

        // Then
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(modelBeanManager, times(1)).getCachedModelVersion(testModelId);
        verify(modelBeanManager, times(1)).updateChatModelBean(eq(testModelId), any(ChatModelEntity.class));
    }

    @Test
    @DisplayName("刷新模型缓存 - Embedding模型")
    public void testRefreshEmbeddingModelCache() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testEmbeddingModelEntity);
        when(modelBeanManager.getCachedModelVersion(testModelId)).thenReturn(0L);
        when(modelBeanManager.updateEmbeddingModelBean(eq(testModelId), any(EmbeddingModelEntity.class)))
                .thenReturn(mockEmbeddingModel);

        // When
        modelService.refreshModelCache(testModelId);

        // Then
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(modelBeanManager, times(1)).getCachedModelVersion(testModelId);
        verify(modelBeanManager, times(1)).updateEmbeddingModelBean(eq(testModelId), any(EmbeddingModelEntity.class));
    }

    @Test
    @DisplayName("获取模型版本状态 - 缓存版本一致")
    public void testGetModelVersionStatusConsistent() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        when(modelBeanManager.getCachedModelVersion(testModelId)).thenReturn(1L);

        // When
        String status = modelService.getModelVersionStatus(testModelId);

        // Then
        assertTrue(status.contains("缓存版本与数据库版本一致"));
        assertTrue(status.contains("版本: 1"));
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(modelBeanManager, times(1)).getCachedModelVersion(testModelId);
    }

    @Test
    @DisplayName("获取模型版本状态 - 缓存版本过期")
    public void testGetModelVersionStatusOutdated() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        when(modelBeanManager.getCachedModelVersion(testModelId)).thenReturn(0L);

        // When
        String status = modelService.getModelVersionStatus(testModelId);

        // Then
        assertTrue(status.contains("缓存版本过期"));
        assertTrue(status.contains("缓存版本: 0，数据库版本: 1"));
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(modelBeanManager, times(1)).getCachedModelVersion(testModelId);
    }

    @Test
    @DisplayName("获取动态表单配置")
    public void testGetFormConfiguration() {
        // Given
        String provider = "ollama";
        String type = "chat";
        FormConfiguration mockConfig = FormConfiguration.builder()
                .provider(provider)
                .type(type)
                .build();
        when(dynamicForm.getFormConfiguration(provider, type)).thenReturn(mockConfig);

        // When
        FormConfiguration result = modelService.getFormConfiguration(provider, type);

        // Then
        assertNotNull(result);
        assertEquals(provider, result.getProvider());
        assertEquals(type, result.getType());
        verify(dynamicForm, times(1)).getFormConfiguration(provider, type);
    }

    @Test
    @DisplayName("校验动态表单数据")
    public void testValidateFormData() {
        // Given
        String provider = "ollama";
        String type = "chat";
        Map<String, Object> formData = new HashMap<>();
        formData.put("modelName", "qwen3");
        formData.put("temperature", 0.7);

        ValidationResult mockResult = ValidationResult.builder()
                .valid(true)
                .build();
        when(dynamicForm.validateFormData(provider, type, formData)).thenReturn(mockResult);

        // When
        ValidationResult result = modelService.validateFormData(provider, type, formData);

        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
        verify(dynamicForm, times(1)).validateFormData(provider, type, formData);
    }

    @Test
    @DisplayName("提交动态表单")
    public void testSubmitForm() {
        // Given
        String provider = "ollama";
        String type = "chat";
        Map<String, Object> formData = new HashMap<>();
        formData.put("modelName", "qwen3");
        formData.put("temperature", 0.7);

        String expectedModelId = UUID.randomUUID().toString();
        when(dynamicForm.submitForm(provider, type, formData)).thenReturn(expectedModelId);

        // When
        String result = modelService.submitForm(provider, type, formData);

        // Then
        assertNotNull(result);
        assertEquals(expectedModelId, result);
        verify(dynamicForm, times(1)).submitForm(provider, type, formData);
    }
} 
package cn.cathead.ai.test.service;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.modelbean.IModelBeanManager;
import cn.cathead.ai.domain.model.service.modelcreation.modelcreationserviceimpl.ModelCreationService;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

/**
 * ModelCreationService 测试
 * 纯单元测试，使用Mock对象
 */
@DisplayName("ModelCreationService 测试")
public class ModelCreationServiceTest {

    @Mock
    private IModelRepository modelRepository;

    @Mock
    private IModelBeanManager modelBeanManager;

    @Mock
    private ChatModel mockChatModel;

    @Mock
    private EmbeddingModel mockEmbeddingModel;

    @InjectMocks
    private ModelCreationService modelCreationService;

    private ChatModelDTO testChatModelDTO;
    private EmbeddingModelDTO testEmbeddingModelDTO;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        setupTestData();
    }

    private void setupTestData() {
        testChatModelDTO = ChatModelDTO.builder()
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)
                .topP(0.9f)
                .maxTokens(2048)
                .presencePenalty(0.0f)
                .frequencyPenalty(0.0f)
                .stop(new String[]{"[INST]", "[/INST]"})
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
    }

    @Test
    @DisplayName("创建Chat模型成功")
    public void testCreateChatModelSuccess() {
        // Given
        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);
        when(modelRepository.saveModelRecord(any(ChatModelEntity.class)))
                .thenReturn(1L); // 返回初始版本号
        doNothing().when(modelBeanManager).saveChatModelToCache(eq(mockChatModel), any(ChatModelEntity.class));

        // When
        modelCreationService.createChatModel(testChatModelDTO);

        // Then
        // 验证创建模型实例被调用
        ArgumentCaptor<ChatModelEntity> entityCaptor = ArgumentCaptor.forClass(ChatModelEntity.class);
        verify(modelBeanManager, times(1)).createChatModelInstance(entityCaptor.capture());
        
        ChatModelEntity capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity.getModelId());
        assertEquals("ollama", capturedEntity.getProviderName());
        assertEquals("qwen3", capturedEntity.getModelName());
        assertEquals("http://localhost:11434", capturedEntity.getUrl());
        assertEquals("", capturedEntity.getKey());
        assertEquals("chat", capturedEntity.getType());
        assertEquals(0.7f, capturedEntity.getTemperature());
        assertEquals(0.9f, capturedEntity.getTopP());
        assertEquals(2048, capturedEntity.getMaxTokens());
        assertArrayEquals(new String[]{"[INST]", "[/INST]"}, capturedEntity.getStop());

        // 验证数据库保存被调用
        verify(modelRepository, times(1)).saveModelRecord(any(ChatModelEntity.class));
        
        // 验证缓存保存被调用
        verify(modelBeanManager, times(1)).saveChatModelToCache(eq(mockChatModel), any(ChatModelEntity.class));
    }

    @Test
    @DisplayName("创建Chat模型失败 - 模型实例创建失败")
    public void testCreateChatModelInstanceCreationFailed() {
        // Given
        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenReturn(null);

        // When
        modelCreationService.createChatModel(testChatModelDTO);

        // Then
        verify(modelBeanManager, times(1)).createChatModelInstance(any(ChatModelEntity.class));
        // 由于创建失败，不应该调用数据库保存和缓存保存
        verify(modelRepository, never()).saveModelRecord(any());
        verify(modelBeanManager, never()).saveChatModelToCache(any(), any());
    }

    @Test
    @DisplayName("创建Embedding模型成功")
    public void testCreateEmbeddingModelSuccess() {
        // Given
        when(modelBeanManager.createEmbeddingModelInstance(any(EmbeddingModelEntity.class)))
                .thenReturn(mockEmbeddingModel);
        when(modelRepository.saveModelRecord(any(EmbeddingModelEntity.class)))
                .thenReturn(1L); // 返回初始版本号
        doNothing().when(modelBeanManager).saveEmbeddingModelToCache(eq(mockEmbeddingModel), any(EmbeddingModelEntity.class));

        // When
        modelCreationService.createEmbeddingModel(testEmbeddingModelDTO);

        // Then
        // 验证创建模型实例被调用
        ArgumentCaptor<EmbeddingModelEntity> entityCaptor = ArgumentCaptor.forClass(EmbeddingModelEntity.class);
        verify(modelBeanManager, times(1)).createEmbeddingModelInstance(entityCaptor.capture());
        
        EmbeddingModelEntity capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity.getModelId());
        assertEquals("ollama", capturedEntity.getProviderName());
        assertEquals("nomic-embed-text", capturedEntity.getModelName());
        assertEquals("http://localhost:11434", capturedEntity.getUrl());
        assertEquals("", capturedEntity.getKey());
        assertEquals("embedding", capturedEntity.getType());
        assertEquals("json", capturedEntity.getEmbeddingFormat());
        assertEquals(512, capturedEntity.getNumPredict());

        // 验证数据库保存被调用
        verify(modelRepository, times(1)).saveModelRecord(any(EmbeddingModelEntity.class));
        
        // 验证缓存保存被调用
        verify(modelBeanManager, times(1)).saveEmbeddingModelToCache(eq(mockEmbeddingModel), any(EmbeddingModelEntity.class));
    }

    @Test
    @DisplayName("创建Embedding模型失败 - 模型实例创建失败")
    public void testCreateEmbeddingModelInstanceCreationFailed() {
        // Given
        when(modelBeanManager.createEmbeddingModelInstance(any(EmbeddingModelEntity.class)))
                .thenReturn(null);

        // When
        modelCreationService.createEmbeddingModel(testEmbeddingModelDTO);

        // Then
        verify(modelBeanManager, times(1)).createEmbeddingModelInstance(any(EmbeddingModelEntity.class));
        // 由于创建失败，不应该调用数据库保存和缓存保存
        verify(modelRepository, never()).saveModelRecord(any());
        verify(modelBeanManager, never()).saveEmbeddingModelToCache(any(), any());
    }

    @Test
    @DisplayName("创建Chat模型 - 验证UUID生成")
    public void testCreateChatModelUUIDGeneration() {
        // Given
        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);
        when(modelRepository.saveModelRecord(any(ChatModelEntity.class)))
                .thenReturn(1L);
        doNothing().when(modelBeanManager).saveChatModelToCache(eq(mockChatModel), any(ChatModelEntity.class));

        // When
        modelCreationService.createChatModel(testChatModelDTO);
        modelCreationService.createChatModel(testChatModelDTO);

        // Then
        ArgumentCaptor<ChatModelEntity> entityCaptor = ArgumentCaptor.forClass(ChatModelEntity.class);
        verify(modelBeanManager, times(2)).createChatModelInstance(entityCaptor.capture());
        
        // 验证两次创建的模型ID不同
        String firstModelId = entityCaptor.getAllValues().get(0).getModelId();
        String secondModelId = entityCaptor.getAllValues().get(1).getModelId();
        assertNotEquals(firstModelId, secondModelId);
        assertNotNull(firstModelId);
        assertNotNull(secondModelId);
    }

    @Test
    @DisplayName("创建Embedding模型 - 验证UUID生成")
    public void testCreateEmbeddingModelUUIDGeneration() {
        // Given
        when(modelBeanManager.createEmbeddingModelInstance(any(EmbeddingModelEntity.class)))
                .thenReturn(mockEmbeddingModel);
        when(modelRepository.saveModelRecord(any(EmbeddingModelEntity.class)))
                .thenReturn(1L);
        doNothing().when(modelBeanManager).saveEmbeddingModelToCache(eq(mockEmbeddingModel), any(EmbeddingModelEntity.class));

        // When
        modelCreationService.createEmbeddingModel(testEmbeddingModelDTO);
        modelCreationService.createEmbeddingModel(testEmbeddingModelDTO);

        // Then
        ArgumentCaptor<EmbeddingModelEntity> entityCaptor = ArgumentCaptor.forClass(EmbeddingModelEntity.class);
        verify(modelBeanManager, times(2)).createEmbeddingModelInstance(entityCaptor.capture());
        
        // 验证两次创建的模型ID不同
        String firstModelId = entityCaptor.getAllValues().get(0).getModelId();
        String secondModelId = entityCaptor.getAllValues().get(1).getModelId();
        assertNotEquals(firstModelId, secondModelId);
        assertNotNull(firstModelId);
        assertNotNull(secondModelId);
    }

    @Test
    @DisplayName("创建Chat模型 - 验证版本号设置")
    public void testCreateChatModelVersionSetting() {
        // Given
        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);
        when(modelRepository.saveModelRecord(any(ChatModelEntity.class)))
                .thenReturn(5L); // 返回特定版本号
        doNothing().when(modelBeanManager).saveChatModelToCache(eq(mockChatModel), any(ChatModelEntity.class));

        // When
        modelCreationService.createChatModel(testChatModelDTO);

        // Then
        ArgumentCaptor<ChatModelEntity> saveEntityCaptor = ArgumentCaptor.forClass(ChatModelEntity.class);
        verify(modelRepository, times(1)).saveModelRecord(saveEntityCaptor.capture());
        
        ArgumentCaptor<ChatModelEntity> cacheEntityCaptor = ArgumentCaptor.forClass(ChatModelEntity.class);
        verify(modelBeanManager, times(1)).saveChatModelToCache(eq(mockChatModel), cacheEntityCaptor.capture());
        
        // 验证保存到缓存的实体包含正确的版本号
        ChatModelEntity cachedEntity = cacheEntityCaptor.getValue();
        assertEquals(5L, cachedEntity.getVersion());
    }

    @Test
    @DisplayName("创建Embedding模型 - 验证版本号设置")
    public void testCreateEmbeddingModelVersionSetting() {
        // Given
        when(modelBeanManager.createEmbeddingModelInstance(any(EmbeddingModelEntity.class)))
                .thenReturn(mockEmbeddingModel);
        when(modelRepository.saveModelRecord(any(EmbeddingModelEntity.class)))
                .thenReturn(3L); // 返回特定版本号
        doNothing().when(modelBeanManager).saveEmbeddingModelToCache(eq(mockEmbeddingModel), any(EmbeddingModelEntity.class));

        // When
        modelCreationService.createEmbeddingModel(testEmbeddingModelDTO);

        // Then
        ArgumentCaptor<EmbeddingModelEntity> saveEntityCaptor = ArgumentCaptor.forClass(EmbeddingModelEntity.class);
        verify(modelRepository, times(1)).saveModelRecord(saveEntityCaptor.capture());
        
        ArgumentCaptor<EmbeddingModelEntity> cacheEntityCaptor = ArgumentCaptor.forClass(EmbeddingModelEntity.class);
        verify(modelBeanManager, times(1)).saveEmbeddingModelToCache(eq(mockEmbeddingModel), cacheEntityCaptor.capture());
        
        // 验证保存到缓存的实体包含正确的版本号
        EmbeddingModelEntity cachedEntity = cacheEntityCaptor.getValue();
        assertEquals(3L, cachedEntity.getVersion());
    }

    @Test
    @DisplayName("创建Chat模型 - 处理空参数")
    public void testCreateChatModelWithNullParameters() {
        // Given
        ChatModelDTO nullParamsDTO = ChatModelDTO.builder()
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                // 其他参数为null
                .build();

        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);
        when(modelRepository.saveModelRecord(any(ChatModelEntity.class)))
                .thenReturn(1L);
        doNothing().when(modelBeanManager).saveChatModelToCache(eq(mockChatModel), any(ChatModelEntity.class));

        // When
        modelCreationService.createChatModel(nullParamsDTO);

        // Then
        ArgumentCaptor<ChatModelEntity> entityCaptor = ArgumentCaptor.forClass(ChatModelEntity.class);
        verify(modelBeanManager, times(1)).createChatModelInstance(entityCaptor.capture());
        
        ChatModelEntity capturedEntity = entityCaptor.getValue();
        assertNull(capturedEntity.getTemperature());
        assertNull(capturedEntity.getTopP());
        assertNull(capturedEntity.getMaxTokens());
        assertNull(capturedEntity.getPresencePenalty());
        assertNull(capturedEntity.getFrequencyPenalty());
        assertNull(capturedEntity.getStop());
    }

    @Test
    @DisplayName("创建Embedding模型 - 处理空参数")
    public void testCreateEmbeddingModelWithNullParameters() {
        // Given
        EmbeddingModelDTO nullParamsDTO = EmbeddingModelDTO.builder()
                .providerName("ollama")
                .modelName("nomic-embed-text")
                .url("http://localhost:11434")
                .key("")
                .type("embedding")
                // 其他参数为null
                .build();

        when(modelBeanManager.createEmbeddingModelInstance(any(EmbeddingModelEntity.class)))
                .thenReturn(mockEmbeddingModel);
        when(modelRepository.saveModelRecord(any(EmbeddingModelEntity.class)))
                .thenReturn(1L);
        doNothing().when(modelBeanManager).saveEmbeddingModelToCache(eq(mockEmbeddingModel), any(EmbeddingModelEntity.class));

        // When
        modelCreationService.createEmbeddingModel(nullParamsDTO);

        // Then
        ArgumentCaptor<EmbeddingModelEntity> entityCaptor = ArgumentCaptor.forClass(EmbeddingModelEntity.class);
        verify(modelBeanManager, times(1)).createEmbeddingModelInstance(entityCaptor.capture());
        
        EmbeddingModelEntity capturedEntity = entityCaptor.getValue();
        assertNull(capturedEntity.getEmbeddingFormat());
        assertNull(capturedEntity.getNumPredict());
    }

    @Test
    @DisplayName("创建模型 - 验证执行顺序")
    public void testCreateModelExecutionOrder() {
        // Given
        when(modelBeanManager.createChatModelInstance(any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);
        when(modelRepository.saveModelRecord(any(ChatModelEntity.class)))
                .thenReturn(1L);
        doNothing().when(modelBeanManager).saveChatModelToCache(eq(mockChatModel), any(ChatModelEntity.class));

        // When
        modelCreationService.createChatModel(testChatModelDTO);

        // Then
        // 验证执行顺序：先创建实例 -> 再保存到数据库 -> 最后保存到缓存
        InOrder inOrder = inOrder(modelBeanManager, modelRepository);
        inOrder.verify(modelBeanManager).createChatModelInstance(any(ChatModelEntity.class));
        inOrder.verify(modelRepository).saveModelRecord(any(ChatModelEntity.class));
        inOrder.verify(modelBeanManager).saveChatModelToCache(eq(mockChatModel), any(ChatModelEntity.class));
    }
} 
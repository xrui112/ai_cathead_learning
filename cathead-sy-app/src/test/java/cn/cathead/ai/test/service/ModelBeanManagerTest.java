package cn.cathead.ai.test.service;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.ModelWrapper;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.modelbean.modelbeanmanagerimpl.ModelBeanManager;
import cn.cathead.ai.domain.model.service.provider.IModelProvider;
import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ModelBeanManager 缓存管理测试
 * 纯单元测试，使用Mock对象，不需要Spring上下文
 */
@DisplayName("ModelBeanManager 缓存管理测试")
public class ModelBeanManagerTest {

    @Mock
    private IModelRepository modelRepository;

    @Mock
    private Cache<String, ModelWrapper<ChatModel>> chatModelCache;

    @Mock
    private Cache<String, ModelWrapper<EmbeddingModel>> embeddingModelCache;

    @Mock
    private Map<String, IModelProvider> modelProviderMap;

    @Mock
    private IModelProvider mockProvider;

    @Mock
    private ChatModel mockChatModel;

    @Mock
    private EmbeddingModel mockEmbeddingModel;

    @InjectMocks
    private ModelBeanManager modelBeanManager;

    private String testModelId;
    private ChatModelEntity testChatModelEntity;
    private EmbeddingModelEntity testEmbeddingModelEntity;
    private ModelWrapper<ChatModel> testChatModelWrapper;
    private ModelWrapper<EmbeddingModel> testEmbeddingModelWrapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        setupTestData();
    }

    private void setupTestData() {
        testModelId = UUID.randomUUID().toString();

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

        testChatModelWrapper = ModelWrapper.<ChatModel>builder()
                .modelInstance(mockChatModel)
                .version(1L)
                .modelId(testModelId)
                .createTime(System.currentTimeMillis())
                .lastAccessTime(System.currentTimeMillis())
                .build();

        testEmbeddingModelWrapper = ModelWrapper.<EmbeddingModel>builder()
                .modelInstance(mockEmbeddingModel)
                .version(1L)
                .modelId(testModelId)
                .createTime(System.currentTimeMillis())
                .lastAccessTime(System.currentTimeMillis())
                .build();
    }

    @Test
    @DisplayName("创建Chat模型实例成功")
    public void testCreateChatModelInstanceSuccess() {
        // Given
        when(modelProviderMap.get("ollama")).thenReturn(mockProvider);
        when(mockProvider.createChat(testChatModelEntity)).thenReturn(mockChatModel);

        // When
        ChatModel result = modelBeanManager.createChatModelInstance(testChatModelEntity);

        // Then
        assertNotNull(result);
        assertEquals(mockChatModel, result);
        verify(modelProviderMap, times(1)).get("ollama");
        verify(mockProvider, times(1)).createChat(testChatModelEntity);
    }

    @Test
    @DisplayName("创建Chat模型实例失败 - 提供者不存在")
    public void testCreateChatModelInstanceProviderNotFound() {
        // Given
        when(modelProviderMap.get("nonexistent")).thenReturn(null);
        testChatModelEntity = ChatModelEntity.builder()
                .modelId(testModelId)
                .providerName("nonexistent")
                .modelName("llama2")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)
                .topP(0.9f)
                .maxTokens(2048)
                .version(1L)
                .build();

        // When
        ChatModel result = modelBeanManager.createChatModelInstance(testChatModelEntity);

        // Then
        assertNull(result);
        verify(modelProviderMap, times(1)).get("nonexistent");
        verify(mockProvider, never()).createChat(any());
    }

    @Test
    @DisplayName("创建Chat模型实例失败 - 异常情况")
    public void testCreateChatModelInstanceException() {
        // Given
        when(modelProviderMap.get("ollama")).thenReturn(mockProvider);
        when(mockProvider.createChat(testChatModelEntity))
                .thenThrow(new RuntimeException("创建模型失败"));

        // When
        ChatModel result = modelBeanManager.createChatModelInstance(testChatModelEntity);

        // Then
        assertNull(result);
        verify(modelProviderMap, times(1)).get("ollama");
        verify(mockProvider, times(1)).createChat(testChatModelEntity);
    }

    @Test
    @DisplayName("创建Embedding模型实例成功")
    public void testCreateEmbeddingModelInstanceSuccess() {
        // Given
        when(modelProviderMap.get("ollama")).thenReturn(mockProvider);
        when(mockProvider.createEmbedding(testEmbeddingModelEntity)).thenReturn(mockEmbeddingModel);

        // When
        EmbeddingModel result = modelBeanManager.createEmbeddingModelInstance(testEmbeddingModelEntity);

        // Then
        assertNotNull(result);
        assertEquals(mockEmbeddingModel, result);
        verify(modelProviderMap, times(1)).get("ollama");
        verify(mockProvider, times(1)).createEmbedding(testEmbeddingModelEntity);
    }

    @Test
    @DisplayName("保存Chat模型到缓存")
    public void testSaveChatModelToCache() {
        // Given
        doNothing().when(chatModelCache).put(eq(testModelId), any(ModelWrapper.class));

        // When
        modelBeanManager.saveChatModelToCache(mockChatModel, testChatModelEntity);

        // Then
        verify(chatModelCache, times(1)).put(eq(testModelId), argThat(wrapper -> {
            assertEquals(mockChatModel, wrapper.getModelInstance());
            assertEquals(testModelId, wrapper.getModelId());
            assertEquals(1L, wrapper.getVersion());
            return true;
        }));
    }

    @Test
    @DisplayName("保存Embedding模型到缓存")
    public void testSaveEmbeddingModelToCache() {
        // Given
        doNothing().when(embeddingModelCache).put(eq(testModelId), any(ModelWrapper.class));

        // When
        modelBeanManager.saveEmbeddingModelToCache(mockEmbeddingModel, testEmbeddingModelEntity);

        // Then
        verify(embeddingModelCache, times(1)).put(eq(testModelId), argThat(wrapper -> {
            assertEquals(mockEmbeddingModel, wrapper.getModelInstance());
            assertEquals(testModelId, wrapper.getModelId());
            assertEquals(1L, wrapper.getVersion());
            return true;
        }));
    }

    @Test
    @DisplayName("从缓存获取Chat模型")
    public void testGetChatModelBean() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(testChatModelWrapper);

        // When
        ChatModel result = modelBeanManager.getChatModelBean(testModelId);

        // Then
        assertNotNull(result);
        assertEquals(mockChatModel, result);
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
        // 验证访问时间被更新
        assertTrue(testChatModelWrapper.getLastAccessTime() > 0);
    }

    @Test
    @DisplayName("从缓存获取Chat模型 - 缓存未命中")
    public void testGetChatModelBeanCacheMiss() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(null);

        // When
        ChatModel result = modelBeanManager.getChatModelBean(testModelId);

        // Then
        assertNull(result);
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
    }

    @Test
    @DisplayName("从缓存获取Embedding模型")
    public void testGetEmbeddingModelBean() {
        // Given
        when(embeddingModelCache.getIfPresent(testModelId)).thenReturn(testEmbeddingModelWrapper);

        // When
        EmbeddingModel result = modelBeanManager.getEmbeddingModelBean(testModelId);

        // Then
        assertNotNull(result);
        assertEquals(mockEmbeddingModel, result);
        verify(embeddingModelCache, times(1)).getIfPresent(testModelId);
    }

    @Test
    @DisplayName("获取Chat模型包装器")
    public void testGetChatModelWrapper() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(testChatModelWrapper);

        // When
        ModelWrapper<ChatModel> result = modelBeanManager.getChatModelWrapper(testModelId);

        // Then
        assertNotNull(result);
        assertEquals(testChatModelWrapper, result);
        assertEquals(testModelId, result.getModelId());
        assertEquals(1L, result.getVersion());
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
    }

    @Test
    @DisplayName("获取Embedding模型包装器")
    public void testGetEmbeddingModelWrapper() {
        // Given
        when(embeddingModelCache.getIfPresent(testModelId)).thenReturn(testEmbeddingModelWrapper);

        // When
        ModelWrapper<EmbeddingModel> result = modelBeanManager.getEmbeddingModelWrapper(testModelId);

        // Then
        assertNotNull(result);
        assertEquals(testEmbeddingModelWrapper, result);
        assertEquals(testModelId, result.getModelId());
        assertEquals(1L, result.getVersion());
        verify(embeddingModelCache, times(1)).getIfPresent(testModelId);
    }

    @Test
    @DisplayName("移除Chat模型Bean")
    public void testRemoveChatModelBean() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(testChatModelWrapper);
        doNothing().when(chatModelCache).invalidate(testModelId);

        // When
        modelBeanManager.removeChatModelBean(testModelId);

        // Then
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
        verify(chatModelCache, times(1)).invalidate(testModelId);
    }

    @Test
    @DisplayName("移除Chat模型Bean - 缓存中不存在")
    public void testRemoveChatModelBeanNotExists() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(null);

        // When
        modelBeanManager.removeChatModelBean(testModelId);

        // Then
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
        verify(chatModelCache, never()).invalidate(testModelId);
    }

    @Test
    @DisplayName("移除Embedding模型Bean")
    public void testRemoveEmbeddingModelBean() {
        // Given
        when(embeddingModelCache.getIfPresent(testModelId)).thenReturn(testEmbeddingModelWrapper);
        doNothing().when(embeddingModelCache).invalidate(testModelId);

        // When
        modelBeanManager.removeEmbeddingModelBean(testModelId);

        // Then
        verify(embeddingModelCache, times(1)).getIfPresent(testModelId);
        verify(embeddingModelCache, times(1)).invalidate(testModelId);
    }

    @Test
    @DisplayName("更新Chat模型Bean")
    public void testUpdateChatModelBean() {
        // Given
        ChatModelEntity updatedEntity = ChatModelEntity.builder()
                .modelId(testModelId)
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.8f)
                .topP(0.9f)
                .maxTokens(2048)
                .version(2L)
                .build();

        when(chatModelCache.getIfPresent(testModelId)).thenReturn(testChatModelWrapper);
        doNothing().when(chatModelCache).invalidate(testModelId);
        when(modelProviderMap.get("ollama")).thenReturn(mockProvider);
        when(mockProvider.createChat(any(ChatModelEntity.class))).thenReturn(mockChatModel);
        when(modelRepository.queryModelById(testModelId)).thenReturn(updatedEntity);
        doNothing().when(chatModelCache).put(eq(testModelId), any(ModelWrapper.class));

        // When
        ChatModel result = modelBeanManager.updateChatModelBean(testModelId, updatedEntity);

        // Then
        assertNotNull(result);
        assertEquals(mockChatModel, result);
        
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
        verify(chatModelCache, times(1)).invalidate(testModelId);
        verify(modelProviderMap, times(1)).get("ollama");
        verify(mockProvider, times(1)).createChat(any(ChatModelEntity.class));
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(chatModelCache, times(1)).put(eq(testModelId), any(ModelWrapper.class));
    }

    @Test
    @DisplayName("更新Embedding模型Bean")
    public void testUpdateEmbeddingModelBean() {
        // Given
        EmbeddingModelEntity updatedEntity = EmbeddingModelEntity.builder()
                .modelId(testModelId)
                .providerName("ollama")
                .modelName("nomic-embed-text")
                .url("http://localhost:11434")
                .key("")
                .type("embedding")
                .embeddingFormat("json")
                .numPredict(1024)
                .version(2L)
                .build();

        when(embeddingModelCache.getIfPresent(testModelId)).thenReturn(testEmbeddingModelWrapper);
        doNothing().when(embeddingModelCache).invalidate(testModelId);
        when(modelProviderMap.get("ollama")).thenReturn(mockProvider);
        when(mockProvider.createEmbedding(any(EmbeddingModelEntity.class))).thenReturn(mockEmbeddingModel);
        when(modelRepository.queryModelById(testModelId)).thenReturn(updatedEntity);
        doNothing().when(embeddingModelCache).put(eq(testModelId), any(ModelWrapper.class));

        // When
        EmbeddingModel result = modelBeanManager.updateEmbeddingModelBean(testModelId, updatedEntity);

        // Then
        assertNotNull(result);
        assertEquals(mockEmbeddingModel, result);
        
        verify(embeddingModelCache, times(1)).getIfPresent(testModelId);
        verify(embeddingModelCache, times(1)).invalidate(testModelId);
        verify(modelProviderMap, times(1)).get("ollama");
        verify(mockProvider, times(1)).createEmbedding(any(EmbeddingModelEntity.class));
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(embeddingModelCache, times(1)).put(eq(testModelId), any(ModelWrapper.class));
    }

    @Test
    @DisplayName("获取所有Chat模型缓存")
    public void testGetAllChatModelCache() {
        // Given
        ConcurrentHashMap<String, ModelWrapper<ChatModel>> cacheMap = new ConcurrentHashMap<>();
        cacheMap.put(testModelId, testChatModelWrapper);
        when(chatModelCache.asMap()).thenReturn(cacheMap);

        // When
        Map<String, ChatModel> result = modelBeanManager.getAllChatModelCache();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(testModelId));
        assertEquals(mockChatModel, result.get(testModelId));
        verify(chatModelCache, times(1)).asMap();
    }

    @Test
    @DisplayName("获取所有Embedding模型缓存")
    public void testGetAllEmbeddingModelCache() {
        // Given
        ConcurrentHashMap<String, ModelWrapper<EmbeddingModel>> cacheMap = new ConcurrentHashMap<>();
        cacheMap.put(testModelId, testEmbeddingModelWrapper);
        when(embeddingModelCache.asMap()).thenReturn(cacheMap);

        // When
        Map<String, EmbeddingModel> result = modelBeanManager.getAllEmbeddingModelCache();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(testModelId));
        assertEquals(mockEmbeddingModel, result.get(testModelId));
        verify(embeddingModelCache, times(1)).asMap();
    }

    @Test
    @DisplayName("清空所有模型Bean")
    public void testClearAllModelBeans() {
        // Given
        doNothing().when(chatModelCache).invalidateAll();
        doNothing().when(embeddingModelCache).invalidateAll();

        // When
        modelBeanManager.clearAllModelBeans();

        // Then
        verify(chatModelCache, times(1)).invalidateAll();
        verify(embeddingModelCache, times(1)).invalidateAll();
    }

    @Test
    @DisplayName("获取缓存模型版本 - Chat模型")
    public void testGetCachedModelVersionForChat() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(testChatModelWrapper);
        when(embeddingModelCache.getIfPresent(testModelId)).thenReturn(null);

        // When
        Long result = modelBeanManager.getCachedModelVersion(testModelId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result);
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
        verify(embeddingModelCache, times(1)).getIfPresent(testModelId);
    }

    @Test
    @DisplayName("获取缓存模型版本 - Embedding模型")
    public void testGetCachedModelVersionForEmbedding() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(null);
        when(embeddingModelCache.getIfPresent(testModelId)).thenReturn(testEmbeddingModelWrapper);

        // When
        Long result = modelBeanManager.getCachedModelVersion(testModelId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result);
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
        verify(embeddingModelCache, times(1)).getIfPresent(testModelId);
    }

    @Test
    @DisplayName("获取缓存模型版本 - 模型不存在")
    public void testGetCachedModelVersionNotFound() {
        // Given
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(null);
        when(embeddingModelCache.getIfPresent(testModelId)).thenReturn(null);

        // When
        Long result = modelBeanManager.getCachedModelVersion(testModelId);

        // Then
        assertNull(result);
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
        verify(embeddingModelCache, times(1)).getIfPresent(testModelId);
    }

    @Test
    @DisplayName("模型包装器访问时间更新")
    public void testModelWrapperAccessTimeUpdate() {
        // Given
        long initialTime = System.currentTimeMillis() - 1000; // 1秒前
        testChatModelWrapper.setLastAccessTime(initialTime);
        when(chatModelCache.getIfPresent(testModelId)).thenReturn(testChatModelWrapper);

        // When
        ChatModel result = modelBeanManager.getChatModelBean(testModelId);

        // Then
        assertNotNull(result);
        assertTrue(testChatModelWrapper.getLastAccessTime() > initialTime);
        verify(chatModelCache, times(1)).getIfPresent(testModelId);
    }
} 
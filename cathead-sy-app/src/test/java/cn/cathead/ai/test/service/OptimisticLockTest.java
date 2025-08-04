package cn.cathead.ai.test.service;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.ModelService;
import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import cn.cathead.ai.types.exception.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 乐观锁并发测试
 * 纯Mock单元测试，不依赖Spring容器
 */
@DisplayName("乐观锁并发测试")
public class OptimisticLockTest {

    @Mock
    private IModelRepository modelRepository;

    @Mock
    private IModelCacheManager modelBeanManager;

    @Mock
    private ChatModel mockChatModel;

    @Mock
    private EmbeddingModel mockEmbeddingModel;

    @InjectMocks
    private ModelService modelService;

    private String testModelId;
    private ChatModelEntity testChatModelEntity;
    private EmbeddingModelEntity testEmbeddingModelEntity;
    private ChatModelDTO testChatModelDTO;
    private EmbeddingModelDTO testEmbeddingModelDTO;

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

        testChatModelDTO = ChatModelDTO.builder()
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.8f) // 修改温度参数
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
                .numPredict(1024) // 修改numPredict参数
                .build();
    }

    @Test
    @DisplayName("单线程更新Chat模型配置成功")
    public void testSingleThreadUpdateChatModelSuccess() {
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
    @DisplayName("乐观锁冲突 - Chat模型更新失败")
    public void testOptimisticLockConflictForChatModel() {
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
        // 由于抛出异常，不应该调用modelBeanManager.updateChatModelBean
        verify(modelBeanManager, never()).updateChatModelBean(any(), any());
    }

    @Test
    @DisplayName("乐观锁冲突 - Embedding模型更新失败")
    public void testOptimisticLockConflictForEmbeddingModel() {
        // Given
        when(modelRepository.queryModelById(testModelId)).thenReturn(testEmbeddingModelEntity);
        doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
                .when(modelRepository).updateModelRecord(any(EmbeddingModelEntity.class));

        // When & Then
        OptimisticLockException exception = assertThrows(OptimisticLockException.class, 
                () -> modelService.updateEmbeddingModelConfig(testModelId, testEmbeddingModelDTO));
        
        assertEquals("模型配置更新失败，数据已被其他用户修改", exception.getMessage());
        verify(modelRepository, times(1)).queryModelById(testModelId);
        verify(modelRepository, times(1)).updateModelRecord(any(EmbeddingModelEntity.class));
        // 由于抛出异常，不应该调用modelBeanManager.updateEmbeddingModelBean
        verify(modelBeanManager, never()).updateEmbeddingModelBean(any(), any());
    }

    @Test
    @DisplayName("并发更新Chat模型配置 - 测试乐观锁机制")
    public void testConcurrentChatModelUpdateWithOptimisticLock() throws InterruptedException {
        // Given
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 设置第一个线程成功，其他线程失败
        when(modelRepository.queryModelById(testModelId)).thenReturn(testChatModelEntity);
        
        // 第一次调用成功，后续调用抛出异常
        doNothing()
            .doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
            .doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
            .doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
            .doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
            .when(modelRepository).updateModelRecord(any(ChatModelEntity.class));

        when(modelBeanManager.updateChatModelBean(eq(testModelId), any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 等待开始信号
                    modelService.updateChatModelConfig(testModelId, testChatModelDTO);
                    successCount.incrementAndGet();
                } catch (OptimisticLockException e) {
                    conflictCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 释放所有线程同时开始
        endLatch.await(); // 等待所有线程完成
        executorService.shutdown();

        // Then
        assertEquals(1, successCount.get(), "应该只有一个线程成功更新");
        assertEquals(4, conflictCount.get(), "应该有4个线程遇到乐观锁冲突");
        
        verify(modelRepository, times(threadCount)).queryModelById(testModelId);
        verify(modelRepository, times(threadCount)).updateModelRecord(any(ChatModelEntity.class));
        verify(modelBeanManager, times(1)).updateChatModelBean(eq(testModelId), any(ChatModelEntity.class));
    }

    @Test
    @DisplayName("并发更新Embedding模型配置 - 测试乐观锁机制")
    public void testConcurrentEmbeddingModelUpdateWithOptimisticLock() throws InterruptedException {
        // Given
        int threadCount = 3;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        when(modelRepository.queryModelById(testModelId)).thenReturn(testEmbeddingModelEntity);
        
        // 第一次调用成功，后续调用抛出异常
        doNothing()
            .doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
            .doThrow(new OptimisticLockException("模型配置更新失败，数据已被其他用户修改"))
            .when(modelRepository).updateModelRecord(any(EmbeddingModelEntity.class));

        when(modelBeanManager.updateEmbeddingModelBean(eq(testModelId), any(EmbeddingModelEntity.class)))
                .thenReturn(mockEmbeddingModel);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    modelService.updateEmbeddingModelConfig(testModelId, testEmbeddingModelDTO);
                    successCount.incrementAndGet();
                } catch (OptimisticLockException e) {
                    conflictCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // Then
        assertEquals(1, successCount.get(), "应该只有一个线程成功更新");
        assertEquals(2, conflictCount.get(), "应该有2个线程遇到乐观锁冲突");
        
        verify(modelRepository, times(threadCount)).queryModelById(testModelId);
        verify(modelRepository, times(threadCount)).updateModelRecord(any(EmbeddingModelEntity.class));
        verify(modelBeanManager, times(1)).updateEmbeddingModelBean(eq(testModelId), any(EmbeddingModelEntity.class));
    }

    @Test
    @DisplayName("模型不存在时更新配置失败")
    public void testUpdateNonExistentModel() {
        // Given
        String nonExistentModelId = "non-existent-model";
        when(modelRepository.queryModelById(nonExistentModelId)).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> modelService.updateChatModelConfig(nonExistentModelId, testChatModelDTO));
        
        assertTrue(exception.getMessage().contains("模型不存在"));
        assertTrue(exception.getMessage().contains(nonExistentModelId));
        
        verify(modelRepository, times(1)).queryModelById(nonExistentModelId);
        verify(modelRepository, never()).updateModelRecord(any());
        verify(modelBeanManager, never()).updateChatModelBean(any(), any());
    }

    @Test
    @DisplayName("版本号校验 - 确保更新时版本号正确传递")
    public void testVersionNumberValidation() {
        // Given
        ChatModelEntity entityWithSpecificVersion = ChatModelEntity.builder()
                .modelId(testModelId)
                .providerName("ollama")
                .modelName("qwen3")
                .url("http://localhost:11434")
                .key("")
                .type("chat")
                .temperature(0.7f)
                .topP(0.9f)
                .maxTokens(2048)
                .version(5L) // 特定版本号
                .build();

        when(modelRepository.queryModelById(testModelId)).thenReturn(entityWithSpecificVersion);
        doNothing().when(modelRepository).updateModelRecord(any(ChatModelEntity.class));
        when(modelRepository.queryModelById(testModelId)).thenReturn(entityWithSpecificVersion);
        when(modelBeanManager.updateChatModelBean(eq(testModelId), any(ChatModelEntity.class)))
                .thenReturn(mockChatModel);

        // When
        modelService.updateChatModelConfig(testModelId, testChatModelDTO);

        // Then
        verify(modelRepository).updateModelRecord(argThat(entity -> {
            // 验证传递给updateModelRecord的实体包含正确的版本号
            assertEquals(5L, entity.getVersion());
            assertEquals(testModelId, entity.getModelId());
            assertEquals(0.8f, ((ChatModelEntity) entity).getTemperature()); // 验证更新的参数
            return true;
        }));
    }
} 
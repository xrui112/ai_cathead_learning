package cn.cathead.ai.domain.model.service;

import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.chat.IChatService;
import cn.cathead.ai.domain.model.service.embedding.IEmbeddingService;
import cn.cathead.ai.domain.model.service.form.IDynamicForm;
import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.domain.model.service.modelcreation.IModelCreationService;
import cn.cathead.ai.domain.model.service.update.impl.ChatModelUpdateService;
import cn.cathead.ai.domain.model.service.update.impl.EmbeddingModelUpdateService;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@Slf4j
public class ModelService implements IModelService {

    @Resource
    private IModelRepository modelRepository;

    // 使用接口来管理模型Bean
    @Resource
    private IModelCacheManager modelBeanManager;

    // 动态表单服务
    @Resource
    private IDynamicForm dynamicForm;

    // 模型创建服务
    @Resource
    private IModelCreationService modelCreationService;

    // 模型更新服务
    @Resource
    private ChatModelUpdateService chatModelUpdateService;

    @Resource
    private EmbeddingModelUpdateService embeddingModelUpdateService;

    // Chat子领域服务
    @Resource
    private IChatService chatService;

    // Embedding子领域服务
    @Resource
    private IEmbeddingService embeddingService;

    /**
     * 流式聊天接口
     */
    @Override
    public Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto) {
        return chatService.chatWithStream(chatRequestDto);
    }

    /**
     * 普通聊天接口
     */
    @Override
    public ChatResponse chatWith(ChatRequestDTO chatRequestDto) {
        return chatService.chatWith(chatRequestDto);
    }

    /**
     * 文本向量化接口
     */
    @Override
    public EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto) {
        return embeddingService.embedText(embeddingRequestDto);
    }

    @Override
    public void deleteModel(String modelId) {
        log.info("开始删除模型，模型ID: {}", modelId);

        // 1. 从ModelBeanManager中移除
        modelBeanManager.removeChatModelBean(modelId);
        modelBeanManager.removeEmbeddingModelBean(modelId);

        // 2. 删除数据库记录
        modelRepository.deleteModelRecord(modelId);

        log.info("模型删除成功，模型ID: {}", modelId);
    }

    @Override
    public BaseModelEntity getModelById(String modelId) {
        return modelRepository.queryModelById(modelId);
    }

    public EmbeddingModel getLatestEmbeddingModel(String modelId) {
        return modelBeanManager.ensureLatestEmbeddingModel(modelId);
    }

    public ChatModel getLatestChatModel(String modelId) {
        return modelBeanManager.ensureLatestChatModel(modelId);
    }

    public String getModelVersionStatus(String modelId) {
        BaseModelEntity dbEntity = modelRepository.queryModelById(modelId);
        if (dbEntity == null) {
            return String.format("模型[%s]不存在", modelId);
        }

        Long cachedVersion = modelBeanManager.getCachedModelVersion(modelId);
        Long dbVersion = dbEntity.getVersion();

        if (cachedVersion == null) {
            return String.format("模型[%s]：缓存中不存在，数据库版本: %d", modelId, dbVersion);
        }

        if (cachedVersion.equals(dbVersion)) {
            return String.format("模型[%s]：缓存版本与数据库版本一致，版本: %d", modelId, dbVersion);
        } else {
            return String.format("模型[%s]：缓存版本过期，缓存版本: %d，数据库版本: %d",
                    modelId, cachedVersion, dbVersion);
        }
    }

    @Override
    public void refreshModelCache(String modelId) {
        modelBeanManager.refreshModelCache(modelId);
    }

    @Override
    public FormConfiguration getFormConfiguration(String provider, String type) {
        log.info("获取动态表单配置，provider: {}, type: {}", provider, type);
        return dynamicForm.getFormConfiguration(provider, type);
    }

    @Override
    public ValidationResult validateFormData(String provider, String type, Map<String, Object> formData) {
        log.info("校验动态表单数据，provider: {}, type: {}", provider, type);
        return dynamicForm.validateFormData(provider, type, formData);
    }

    @Override
    public String submitForm(String provider, String type, Map<String, Object> formData) {
        log.info("提交动态表单，provider: {}, type: {}", provider, type);
        return dynamicForm.submitForm(provider, type, formData);
    }

    @Override
    public void updateChatModelConfigByFormData(String modelId, String provider, Map<String, Object> formData) {
        chatModelUpdateService.updateModelByFormData(modelId, provider, formData);
    }

    @Override
    public void updateEmbeddingModelConfigByFormData(String modelId, String provider, Map<String, Object> formData) {
        embeddingModelUpdateService.updateModelByFormData(modelId, provider, formData);
    }
}

package cn.cathead.ai.domain.model.service;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.ChatRequestDto;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatRequestEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.DynamicForm.IDynamicForm;
import cn.cathead.ai.domain.model.service.ModelBean.IModelBeanManager;
import cn.cathead.ai.domain.model.service.ModelCreation.IModelCreationService;
import cn.cathead.ai.domain.model.service.provider.IModelProvider;
import cn.cathead.ai.types.exception.OptimisticLockException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class ModelService implements IModelService {

    @Resource
    private Map<String, IModelProvider> modelProviderMap = new ConcurrentHashMap<>();

    @Resource
    private IModelRepository iModelRepository;

    // 使用接口来管理模型Bean
    @Resource
    private IModelBeanManager modelBeanManager;

    // 动态表单服务
    @Resource
    private IDynamicForm dynamicForm;
    
    // 模型创建服务
    @Resource
    private IModelCreationService modelCreationService;

    @Override
    public void creatModel(ChatModelDTO chatModelDTO) {
        modelCreationService.createChatModel(chatModelDTO);
    }

    @Override
    public void creatModel(EmbeddingModelDTO embeddingModelDTO) {
        modelCreationService.createEmbeddingModel(embeddingModelDTO);
    }

    /**
     *  对应使用chatModel
     * @param chatRequestDto
     * @return
     *
     * 需要注意的是,调用该接口之前,已经默认已经createModel 处理过了
     *
     */
    @Override
    public Flux<ChatResponse> chatWith(ChatRequestDto chatRequestDto) {
        ChatRequestEntity chatRequestEntity = ChatRequestEntity.builder()
                .modelId(chatRequestDto.getModelId())
                .prompt(chatRequestDto.getPrompt())
                .build();

        // !!!!!!先检查并确保缓存是最新版本 所有的model使用 都要先ensureLatestChatModel检查version
        ChatModel chatModel = ensureLatestChatModel(chatRequestDto.getModelId());
        
        if (chatModel != null) {
            return generateStream(chatModel, chatRequestDto.getPrompt());
        }
        
        log.error("未找到模型，模型ID: {}", chatRequestDto.getModelId());
        return Flux.empty();
    }

    public Flux<ChatResponse> generateStream(ChatModel chatModel,String message) {
        log.info("调用流式接口");
        return chatModel.stream(
                new Prompt(
                    message
                )
        );
    }

    @Override
    public void updateChatModelConfig(String modelId, ChatModelDTO chatModelDTO) {
        log.info("开始更新Chat模型配置，模型ID: {}", modelId);
        BaseModelEntity currentEntity = iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            throw new IllegalArgumentException("模型不存在，模型ID: " + modelId);
        }

        // 1. 构建新的ChatModelEntity
        ChatModelEntity chatModelEntity = ChatModelEntity.builder()
                .modelId(modelId) // 保持原有ID
                .providerName(chatModelDTO.getProviderName())
                .modelName(chatModelDTO.getModelName())
                .url(chatModelDTO.getUrl())
                .key(chatModelDTO.getKey())
                .type(chatModelDTO.getType())
                .temperature(chatModelDTO.getTemperature())
                .topP(chatModelDTO.getTopP())
                .maxTokens(chatModelDTO.getMaxTokens())
                .presencePenalty(chatModelDTO.getPresencePenalty())
                .frequencyPenalty(chatModelDTO.getFrequencyPenalty())
                .stop(chatModelDTO.getStop())
                .version(currentEntity.getVersion())
                .build();

        // 3. 尝试更新（可能抛出OptimisticLockException）
    try {
        iModelRepository.updateModelRecord(chatModelEntity);

        BaseModelEntity updatedEntity = iModelRepository.queryModelById(modelId);

        // 4. 更新成功，刷新内存中的模型Bean
        modelBeanManager.updateChatModelBean(modelId, (ChatModelEntity) updatedEntity);
        log.info("Chat模型配置更新成功，模型ID: {}", modelId);
        
    } catch (OptimisticLockException e) {
        log.warn("Chat模型配置更新失败，存在并发冲突，模型ID: {}", modelId);
        throw e; // 重新抛出异常让Controller处理
    }
    }

    @Override
    public void updateEmbeddingModelConfig(String modelId, EmbeddingModelDTO embeddingModelDTO) {
        log.info("开始更新Embedding模型配置，模型ID: {}", modelId);
        BaseModelEntity currentEntity = iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            throw new IllegalArgumentException("模型不存在，模型ID: " + modelId);
        }
        
        // 1. 构建新的EmbeddingModelEntity
        EmbeddingModelEntity embeddingModelEntity = EmbeddingModelEntity.builder()
                .modelId(modelId) // 保持原有ID
                .providerName(embeddingModelDTO.getProviderName())
                .modelName(embeddingModelDTO.getModelName())
                .url(embeddingModelDTO.getUrl())
                .key(embeddingModelDTO.getKey())
                .type(embeddingModelDTO.getType())
                .embeddingFormat(embeddingModelDTO.getEmbeddingFormat())
                .numPredict(embeddingModelDTO.getNumPredict())
                .version(currentEntity.getVersion()) // 设置当前版本号
                .build();
        
        // 2. 尝试更新（可能抛出OptimisticLockException）
        try {
        iModelRepository.updateModelRecord(embeddingModelEntity);

        BaseModelEntity updatedEntity = iModelRepository.queryModelById(modelId);
        
        // 3. 使用ModelBeanManager更新模型Bean
        EmbeddingModel newEmbeddingModel = modelBeanManager.updateEmbeddingModelBean(modelId, (EmbeddingModelEntity) updatedEntity);
        
        if (newEmbeddingModel != null) {
            log.info("Embedding模型配置更新成功，模型ID: {}", modelId);
        } else {
            log.error("Embedding模型配置更新失败，无法创建新模型，模型ID: {}", modelId);
            }
        } catch (OptimisticLockException e) {
            log.warn("Embedding模型配置更新失败，存在并发冲突，模型ID: {}", modelId);
            throw e; // 重新抛出异常让Controller处理
        }
    }

    @Override
    public void deleteModel(String modelId) {
        log.info("开始删除模型，模型ID: {}", modelId);
        
        // 1. 从ModelBeanManager中移除
        modelBeanManager.removeChatModelBean(modelId);
        modelBeanManager.removeEmbeddingModelBean(modelId);
        
        // 2. 删除数据库记录
        iModelRepository.deleteModelRecord(modelId);
        
        log.info("模型删除成功，模型ID: {}", modelId);
    }

    @Override
    public BaseModelEntity getModelById(String modelId) {
        return iModelRepository.queryModelById(modelId);
    }

    public EmbeddingModel getLatestEmbeddingModel(String modelId) {
        return ensureLatestEmbeddingModel(modelId);
    }

    public ChatModel getLatestChatModel(String modelId) {
        return ensureLatestChatModel(modelId);
    }

    public String getModelVersionStatus(String modelId) {
        BaseModelEntity dbEntity = iModelRepository.queryModelById(modelId);
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

    private ChatModel ensureLatestChatModel(String modelId) {
        log.debug("检查Chat模型版本，模型ID: {}", modelId);
        // 1. 从数据库获取当前版本
        ChatModelEntity currentEntity = (ChatModelEntity) iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            log.warn("模型不存在，清理缓存，模型ID: {}", modelId);
            modelBeanManager.removeChatModelBean(modelId);
            return null;
        }
        // 2. 获取缓存版本信息
        Long cachedVersion = modelBeanManager.getCachedModelVersion(modelId);
        // 3. 判断是否需要更新缓存
        if (cachedVersion == null) {
            // 3.1缓存中没有，直接创建
            log.debug("缓存中没有模型，创建新模型，模型ID: {}", modelId);
            return modelBeanManager.updateChatModelBean(modelId, currentEntity);
        } else if (cachedVersion.equals(currentEntity.getVersion())) {
            // 3.2版本一致，直接返回
            log.debug("缓存版本是最新的，直接返回缓存模型，模型ID: {}", modelId);
            return modelBeanManager.getChatModelBean(modelId);
        } else {
            // 3.3版本过期，需要更新
            log.info("缓存版本过期，更新Chat模型缓存，模型ID: {}, 数据库版本: {}, 缓存版本: {}",
                    modelId, currentEntity.getVersion(), cachedVersion);
            return modelBeanManager.updateChatModelBean(modelId, currentEntity);
        }
    }


    private EmbeddingModel ensureLatestEmbeddingModel(String modelId) {
        log.debug("检查Embedding模型版本，模型ID: {}", modelId);

        // 1. 从数据库获取当前版本
        EmbeddingModelEntity currentEntity = (EmbeddingModelEntity) iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            log.warn("模型不存在，清理缓存，模型ID: {}", modelId);
            modelBeanManager.removeEmbeddingModelBean(modelId);
            return null;
        }

        // 2. 获取缓存版本信息
        Long cachedVersion = modelBeanManager.getCachedModelVersion(modelId);

        // 3. 判断是否需要更新缓存
        if (cachedVersion == null) {
            // 缓存中没有，直接创建
            log.debug("缓存中没有模型，创建新模型，模型ID: {}", modelId);
            return modelBeanManager.updateEmbeddingModelBean(modelId, currentEntity);
        } else if (cachedVersion.equals(currentEntity.getVersion())) {
            // 版本一致，直接返回
            log.debug("缓存版本是最新的，直接返回缓存模型，模型ID: {}", modelId);
            return modelBeanManager.getEmbeddingModelBean(modelId);
        } else {
            // 版本过期，需要更新
            log.info("缓存版本过期，更新Embedding模型缓存，模型ID: {}, 数据库版本: {}, 缓存版本: {}",
                    modelId, currentEntity.getVersion(), cachedVersion);
            return modelBeanManager.updateEmbeddingModelBean(modelId, currentEntity);
        }
    }

    @Override
    public void refreshModelCache(String modelId) {
        log.info("开始强制刷新模型Bean，模型ID: {}", modelId);
        
        // 1. 从数据库重新加载模型信息
        BaseModelEntity modelEntity = iModelRepository.queryModelById(modelId);
        if (modelEntity == null) {
            log.warn("模型不存在，无法刷新Bean，模型ID: {}", modelId);
            return;
        }
        
        // 2. 强制更新缓存，不进行版本检查
        log.info("强制刷新模型缓存，模型ID: {}, 数据库版本: {}, 原缓存版本: {}", 
                modelId, modelEntity.getVersion(), modelBeanManager.getCachedModelVersion(modelId));
        
        // 3. 使用ModelBeanManager重新创建模型Bean
        if ("chat".equalsIgnoreCase(modelEntity.getType())) {
            ChatModel chatModel = modelBeanManager.updateChatModelBean(modelId, (ChatModelEntity) modelEntity);
            if (chatModel != null) {
                log.info("Chat模型Bean强制刷新成功，模型ID: {}, 新版本: {}", 
                        modelId, modelEntity.getVersion());
            }
        } else if ("embedding".equalsIgnoreCase(modelEntity.getType())) {
            EmbeddingModel embeddingModel = modelBeanManager.updateEmbeddingModelBean(modelId, (EmbeddingModelEntity) modelEntity);
            if (embeddingModel != null) {
                log.info("Embedding模型Bean强制刷新成功，模型ID: {}, 新版本: {}", 
                        modelId, modelEntity.getVersion());
            }
        } else {
            log.warn("未知的模型类型，无法刷新，模型ID: {}, 类型: {}", modelId, modelEntity.getType());
        }
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
}

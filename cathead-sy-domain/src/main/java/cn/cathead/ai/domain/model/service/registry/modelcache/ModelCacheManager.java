package cn.cathead.ai.domain.model.service.registry.modelcache;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.ModelWrapper;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.registry.provider.IModelProvider;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;

/**
 * 模型Bean配置类
 * 负责动态管理模型Bean的创建和销毁
 */
@Component
@Slf4j
public class ModelCacheManager implements IModelCacheManager {

    @Resource
    private Map<String, IModelProvider> modelProviderMap;

    @Autowired
    @Qualifier("chatModelCache")
    private Cache<String, ModelWrapper<ChatModel>> chatModelCache;

    @Autowired
    @Qualifier("embeddingModelCache")
    private Cache<String, ModelWrapper<EmbeddingModel>> embeddingModelCache;

    @Resource
    private IModelRepository iModelRepository;

    @Override
    public ChatModel createChatModelInstance(ChatModelEntity chatModelEntity) {
        try {
            IModelProvider modelProvider = modelProviderMap.get(chatModelEntity.getProviderName().toLowerCase());
            if (modelProvider == null) {
                log.error("未找到模型提供者: {}", chatModelEntity.getProviderName());
                return null;
            }
            ChatModel chatModel = modelProvider.createChat(chatModelEntity);
            log.info("成功创建Chat模型实例，模型ID: {}", chatModelEntity.getModelId());
            return chatModel;
        } catch (Exception e) {
            log.error("创建Chat模型实例失败，模型ID: {}, 错误: {}", chatModelEntity.getModelId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public EmbeddingModel createEmbeddingModelInstance(EmbeddingModelEntity embeddingModelEntity) {
        try {
            IModelProvider modelProvider = modelProviderMap.get(embeddingModelEntity.getProviderName().toLowerCase());
            if (modelProvider == null) {
                log.error("未找到模型提供者: {}", embeddingModelEntity.getProviderName());
                return null;
            }

            EmbeddingModel embeddingModel = modelProvider.createEmbedding(embeddingModelEntity);
            if (embeddingModel != null) {
                log.info("成功创建Embedding模型实例，模型ID: {}", embeddingModelEntity.getModelId());
            }
            return embeddingModel;
        } catch (Exception e) {
            log.error("创建Embedding模型实例失败，模型ID: {}, 错误: {}", embeddingModelEntity.getModelId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void saveChatModelToCache(ChatModel chatModel, ChatModelEntity chatModelEntity) {
        ModelWrapper<ChatModel> wrapper = ModelWrapper.<ChatModel>builder()
                .modelInstance(chatModel)
                .version(chatModelEntity.getVersion())
                .modelId(chatModelEntity.getModelId())
                .createTime(System.currentTimeMillis())
                .lastAccessTime(System.currentTimeMillis())
                .build();
        
        chatModelCache.put(chatModelEntity.getModelId(), wrapper);
        log.info("成功将Chat模型存入缓存，模型ID: {}, 版本: {}", chatModelEntity.getModelId(), chatModelEntity.getVersion());
    }

    @Override
    public void saveEmbeddingModelToCache(EmbeddingModel embeddingModel, EmbeddingModelEntity embeddingModelEntity) {
        ModelWrapper<EmbeddingModel> wrapper = ModelWrapper.<EmbeddingModel>builder()
                .modelInstance(embeddingModel)
                .version(embeddingModelEntity.getVersion())
                .modelId(embeddingModelEntity.getModelId())
                .createTime(System.currentTimeMillis())
                .lastAccessTime(System.currentTimeMillis())
                .build();
        
        embeddingModelCache.put(embeddingModelEntity.getModelId(), wrapper);
        log.info("成功将Embedding模型存入缓存，模型ID: {}, 版本: {}", embeddingModelEntity.getModelId(), embeddingModelEntity.getVersion());
    }

    @Override
    public ChatModel getChatModelBean(String modelId) {
        ModelWrapper<ChatModel> wrapper = chatModelCache.getIfPresent(modelId);
        if (wrapper != null) {
            wrapper.updateLastAccessTime();
            return wrapper.getModelInstance();
        }
        return null;
    }

    @Override
    public EmbeddingModel getEmbeddingModelBean(String modelId) {
        ModelWrapper<EmbeddingModel> wrapper = embeddingModelCache.getIfPresent(modelId);
        if (wrapper != null) {
            wrapper.updateLastAccessTime();
            return wrapper.getModelInstance();
        }
        return null;
    }

    @Override
    public ModelWrapper<ChatModel> getChatModelWrapper(String modelId) {
        ModelWrapper<ChatModel> wrapper = chatModelCache.getIfPresent(modelId);
        if (wrapper != null) {
            wrapper.updateLastAccessTime();
        }
        return wrapper;
    }

    @Override
    public ModelWrapper<EmbeddingModel> getEmbeddingModelWrapper(String modelId) {
        ModelWrapper<EmbeddingModel> wrapper = embeddingModelCache.getIfPresent(modelId);
        if (wrapper != null) {
            wrapper.updateLastAccessTime();
        }
        return wrapper;
    }

    @Override
    public void removeChatModelBean(String modelId) {
        ModelWrapper<ChatModel> removed = chatModelCache.getIfPresent(modelId);
        if (removed != null) {
            chatModelCache.invalidate(modelId);
            log.info("成功移除Chat模型Bean，模型ID: {}", modelId);
        }
    }

    @Override
    public void removeEmbeddingModelBean(String modelId) {
        ModelWrapper<EmbeddingModel> removed = embeddingModelCache.getIfPresent(modelId);
        if (removed != null) {
            embeddingModelCache.invalidate(modelId);
            log.info("成功移除Embedding模型Bean，模型ID: {}", modelId);
        }
    }

    /**
     *
     * @param modelId 模型ID
     * @param chatModelEntity 新的模型实体
     * @return 创建新的model到缓存中
     */
    @Override
    public ChatModel updateChatModelBean(String modelId, ChatModelEntity chatModelEntity) {
        // 先移除旧的Bean
        removeChatModelBean(modelId);
        // 创建新的实例
        ChatModel chatModel = createChatModelInstance(chatModelEntity);
        if (chatModel != null) {
            // 从数据库获取最新版本信息
            ChatModelEntity latestEntity = (ChatModelEntity) iModelRepository.queryModelById(modelId);
            if (latestEntity != null) {
                // 使用最新的实体信息（包含正确的版本号）存入缓存
                saveChatModelToCache(chatModel, latestEntity);
            } else {
                // 如果数据库中没有找到，使用传入的实体（向后兼容）
                saveChatModelToCache(chatModel, chatModelEntity);
            }
        }
        return chatModel;
    }

    @Override
    public EmbeddingModel updateEmbeddingModelBean(String modelId, EmbeddingModelEntity embeddingModelEntity) {
        // 先移除旧的Bean
        removeEmbeddingModelBean(modelId);
        // 创建新的实例
        EmbeddingModel embeddingModel = createEmbeddingModelInstance(embeddingModelEntity);
        if (embeddingModel != null) {
            // 从数据库获取最新版本信息
            EmbeddingModelEntity latestEntity = (EmbeddingModelEntity) iModelRepository.queryModelById(modelId);
            if (latestEntity != null) {
                // 使用最新的实体信息（包含正确的版本号）存入缓存
                saveEmbeddingModelToCache(embeddingModel, latestEntity);
            } else {
                // 如果数据库中没有找到，使用传入的实体（向后兼容）
                saveEmbeddingModelToCache(embeddingModel, embeddingModelEntity);
            }
        }
        return embeddingModel;
    }

    @Override
    public Map<String, ChatModel> getAllChatModelCache() {
        Map<String, ChatModel> chatModels = new ConcurrentHashMap<>();
        chatModelCache.asMap().forEach((key, wrapper) -> {
            chatModels.put(key, wrapper.getModelInstance());
        });
        return chatModels;
    }

    @Override
    public Map<String, EmbeddingModel> getAllEmbeddingModelCache() {
        Map<String, EmbeddingModel> embeddingModels = new ConcurrentHashMap<>();
        embeddingModelCache.asMap().forEach((key, wrapper) -> {
            embeddingModels.put(key, wrapper.getModelInstance());
        });
        return embeddingModels;
    }

    @Override
    public void clearAllModelBeans() {
        chatModelCache.invalidateAll();
        embeddingModelCache.invalidateAll();
        log.info("已清空所有模型Bean");
    }

    @Override
    public String getModelBeanStats() {
        CacheStats chatStats = chatModelCache.stats();
        CacheStats embeddingStats = embeddingModelCache.stats();
        
        return String.format(
            "Chat模型缓存: size=%d, hitRate=%.2f%%, Embedding模型缓存: size=%d, hitRate=%.2f%%",
            chatModelCache.size(), chatStats.hitRate() * 100,
            embeddingModelCache.size(), embeddingStats.hitRate() * 100
        );
    }

    @Override
    public Long getCachedModelVersion(String modelId) {
        // 先检查Chat模型缓存
        ModelWrapper<ChatModel> chatWrapper = chatModelCache.getIfPresent(modelId);
        if (chatWrapper != null) {
            return chatWrapper.getVersion();
        }
        // 再检查Embedding模型缓存
        ModelWrapper<EmbeddingModel> embeddingWrapper = embeddingModelCache.getIfPresent(modelId);
        if (embeddingWrapper != null) {
            return embeddingWrapper.getVersion();
        }
        
        return null; // 缓存中不存在
    }

    @Override
    public ChatModel ensureLatestChatModel(String modelId) {
        log.debug("检查Chat模型版本，模型ID: {}", modelId);
        
        // 1. 从数据库获取当前版本
        ChatModelEntity currentEntity = (ChatModelEntity) iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            log.warn("模型不存在，清理缓存，模型ID: {}", modelId);
            removeChatModelBean(modelId);
            throw new AppException(ResponseCode.MODEL_NOT_FOUND.getCode(),ResponseCode.MODEL_NOT_FOUND.getInfo());
        }
        
        // 2. 获取缓存版本信息
        Long cachedVersion = getCachedModelVersion(modelId);
        
        // 3. 判断是否需要更新缓存
        if (cachedVersion == null) {
            // 缓存中没有，直接创建
            log.debug("缓存中没有模型，创建新模型，模型ID: {}", modelId);
            return updateChatModelBean(modelId, currentEntity);
        } else if (cachedVersion.equals(currentEntity.getVersion())) {
            // 版本一致，直接返回
            log.debug("缓存版本是最新的，直接返回缓存模型，模型ID: {}", modelId);
            return getChatModelBean(modelId);
        } else {
            // 版本过期，需要更新
            log.info("缓存版本过期，更新Chat模型缓存，模型ID: {}, 数据库版本: {}, 缓存版本: {}",
                    modelId, currentEntity.getVersion(), cachedVersion);
            return updateChatModelBean(modelId, currentEntity);
        }
    }

    @Override
    public EmbeddingModel ensureLatestEmbeddingModel(String modelId) {
        log.debug("检查Embedding模型版本，模型ID: {}", modelId);
        
        // 1. 从数据库获取当前版本
        EmbeddingModelEntity currentEntity = (EmbeddingModelEntity) iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            log.warn("模型不存在，清理缓存，模型ID: {}", modelId);
            removeEmbeddingModelBean(modelId);
            return null;
        }
        
        // 2. 获取缓存版本信息
        Long cachedVersion = getCachedModelVersion(modelId);
        
        // 3. 判断是否需要更新缓存
        if (cachedVersion == null) {
            // 缓存中没有，直接创建
            log.debug("缓存中没有模型，创建新模型，模型ID: {}", modelId);
            return updateEmbeddingModelBean(modelId, currentEntity);
        } else if (cachedVersion.equals(currentEntity.getVersion())) {
            // 版本一致，直接返回
            log.debug("缓存版本是最新的，直接返回缓存模型，模型ID: {}", modelId);
            return getEmbeddingModelBean(modelId);
        } else {
            // 版本过期，需要更新
            log.info("缓存版本过期，更新Embedding模型缓存，模型ID: {}, 数据库版本: {}, 缓存版本: {}",
                    modelId, currentEntity.getVersion(), cachedVersion);
            return updateEmbeddingModelBean(modelId, currentEntity);
        }
    }

    @Override
    public void refreshModelCache(String modelId) {
        log.info("开始强制刷新模型Bean，模型ID: {}", modelId);

        // 1. 从数据库重新加载模型信息
        BaseModelEntity modelEntity = iModelRepository.queryModelById(modelId);
        if (modelEntity == null) {
            log.warn("模型不存在，无法刷新Bean，模型ID: {}", modelId);
            throw new RuntimeException("模型不存在，无法刷新Bean，模型ID: " + modelId);
        }

        // 2. 强制更新缓存，不进行版本检查
        log.info("强制刷新模型缓存，模型ID: {}, 数据库版本: {}, 原缓存版本: {}",
                modelId, modelEntity.getVersion(), getCachedModelVersion(modelId));

        // 3. 使用ModelCacheManager重新创建模型Bean
        if ("chat".equalsIgnoreCase(modelEntity.getType())) {
            ChatModel chatModel = updateChatModelBean(modelId, (ChatModelEntity) modelEntity);
            if (chatModel != null) {
                log.info("Chat模型Bean强制刷新成功，模型ID: {}, 新版本: {}",
                        modelId, modelEntity.getVersion());
            }
        } else if ("embedding".equalsIgnoreCase(modelEntity.getType())) {
            EmbeddingModel embeddingModel = updateEmbeddingModelBean(modelId, (EmbeddingModelEntity) modelEntity);
            if (embeddingModel != null) {
                log.info("Embedding模型Bean强制刷新成功，模型ID: {}, 新版本: {}",
                        modelId, modelEntity.getVersion());
            }
        } else {
            log.warn("未知的模型类型，无法刷新，模型ID: {}, 类型: {}", modelId, modelEntity.getType());
        }
    }
}

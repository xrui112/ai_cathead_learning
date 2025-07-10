package cn.cathead.ai.domain.model.service.ModelBean;

import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.ModelWrapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Map;

/**
 * 模型Bean管理接口
 * 定义模型Bean的创建、获取、更新、删除等操作
 */
public interface IModelBeanManager {

    /**
     * 创建Chat模型实例
     * @param chatModelEntity Chat模型实体
     * @return Chat模型实例
     */
    ChatModel createChatModelInstance(ChatModelEntity chatModelEntity);

    /**
     * 创建Embedding模型实例
     * @param embeddingModelEntity Embedding模型实体
     * @return Embedding模型实例
     */
    EmbeddingModel createEmbeddingModelInstance(EmbeddingModelEntity embeddingModelEntity);

    /**
     * 将Chat模型存入缓存
     * @param chatModel 模型实例
     * @param chatModelEntity 模型实体（包含版本信息）
     */
    void saveChatModelToCache(ChatModel chatModel, ChatModelEntity chatModelEntity);

    /**
     * 将Embedding模型存入缓存
     * @param embeddingModel 模型实例
     * @param embeddingModelEntity 模型实体（包含版本信息）
     */
    void saveEmbeddingModelToCache(EmbeddingModel embeddingModel, EmbeddingModelEntity embeddingModelEntity);

    /**
     * 获取Chat模型Bean
     * @param modelId 模型ID
     * @return Chat模型实例
     */
    ChatModel getChatModelBean(String modelId);

    /**
     * 获取Embedding模型Bean
     * @param modelId 模型ID
     * @return Embedding模型实例
     */
    EmbeddingModel getEmbeddingModelBean(String modelId);

    /**
     * 获取Chat模型包装器
     * @param modelId 模型ID
     * @return Chat模型包装器
     */
    ModelWrapper<ChatModel> getChatModelWrapper(String modelId);

    /**
     * 获取Embedding模型包装器
     * @param modelId 模型ID
     * @return Embedding模型包装器
     */
    ModelWrapper<EmbeddingModel> getEmbeddingModelWrapper(String modelId);


    /**
     * 移除Chat模型Bean
     * @param modelId 模型ID
     */
    void removeChatModelBean(String modelId);

    /**
     * 移除Embedding模型Bean
     * @param modelId 模型ID
     */
    void removeEmbeddingModelBean(String modelId);

    /**
     * 更新Chat模型Bean
     * @param modelId 模型ID
     * @param chatModelEntity 新的模型实体
     * @return 更新后的Chat模型实例
     */
    ChatModel updateChatModelBean(String modelId, ChatModelEntity chatModelEntity);

    /**
     * 更新Embedding模型Bean
     * @param modelId 模型ID
     * @param embeddingModelEntity 新的模型实体
     * @return 更新后的Embedding模型实例
     */
    EmbeddingModel updateEmbeddingModelBean(String modelId, EmbeddingModelEntity embeddingModelEntity);

    /**
     * 获取所有Chat模型Bean
     * @return Chat模型Bean映射
     */
    Map<String, ChatModel> getAllChatModelCache();

    /**
     * 获取所有Embedding模型Bean
     * @return Embedding模型Bean映射
     */
    Map<String, EmbeddingModel> getAllEmbeddingModelCache();

    /**
     * 清空所有模型Bean
     */
    void clearAllModelBeans();

    /**
     * 获取模型Bean统计信息
     * @return 统计信息
     */
    String getModelBeanStats();

    /**
     * 获取缓存中模型的版本号
     * @param modelId 模型ID
     * @return 版本号，如果不存在返回null
     */
    Long getCachedModelVersion(String modelId);

} 
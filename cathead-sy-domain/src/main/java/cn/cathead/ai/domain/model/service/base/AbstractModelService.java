package cn.cathead.ai.domain.model.service.base;

import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;

/**
 * 封装Chat和Embedding服务的共同逻辑
 */
@Slf4j
public abstract class AbstractModelService<T> {

    protected final IModelCacheManager modelCacheManager;
    protected final String modelTypeName;

    protected AbstractModelService(IModelCacheManager modelCacheManager, String modelTypeName) {
        this.modelCacheManager = modelCacheManager;
        this.modelTypeName = modelTypeName;
    }

    /**
     * 获取并验证模型
     */
    protected T getAndValidateModel(String modelId) {
        log.info("调用{}接口，模型ID: {}", modelTypeName, modelId);
        return getModelFromCache(modelId);
    }

    protected abstract T getModelFromCache(String modelId);

    /**
     * 处理模型调用异常
     */
    protected AppException handleModelCallException(String operation, Exception e) {
        String errorMsg = String.format("%s处理失败: %s", operation, e.getMessage());
        log.error(errorMsg, e);
        return createModelCallException(errorMsg);
    }

    /**
     * 创建模型调用异常
     */
    protected abstract AppException createModelCallException(String message);
}
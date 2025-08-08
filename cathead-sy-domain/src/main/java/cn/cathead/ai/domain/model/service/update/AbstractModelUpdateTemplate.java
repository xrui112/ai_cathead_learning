package cn.cathead.ai.domain.model.service.update;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.form.IDynamicForm;
import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.types.exception.OptimisticLockException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
public abstract class AbstractModelUpdateTemplate<T extends BaseModelEntity, D> {

    @Resource
    protected IModelRepository modelRepository;

    @Resource
    protected IModelCacheManager modelCacheManager;


    @Resource
    protected IDynamicForm dynamicForm;


    /**
     * @param modelId 模型ID
     * @param provider 提供商
     * @param formData 表单数据
     */
    public final void updateModelByFormData(String modelId, String provider, Map<String, Object> formData) {
        log.info("开始使用formData更新{}模型配置，模型ID: {}, provider: {}", getModelType(), modelId, provider);
        
        // 1. 前置验证 从数据库里查出
        BaseModelEntity currentEntity = validateAndGetCurrentEntity(modelId);
        validateModelType(currentEntity);
        
        // 2. 校验表单数据 调用校验器
        ValidationResult validationResult = validateFormData(provider, getModelType(), formData);
        if (!validationResult.isValid()) {
            log.error("表单数据校验失败: {}", validationResult.getAllErrors());
            throw new RuntimeException("表单数据校验失败: " + validationResult.getAllErrors());
        }
        
        // 3. 在这里应用默认值 后面构建实体就可以正常构建
        Map<String, Object> formDataWithDefaults = applyDefaultValues(provider, getModelType(), formData);
        log.debug("应用默认值后的表单数据: {}", formDataWithDefaults);

        // 4. 构建实体并更新
        T updatedEntity = buildEntityFromFormData(modelId, provider, formDataWithDefaults, currentEntity.getVersion());
        executeUpdate(modelId, updatedEntity);
        
        log.info("{}模型配置更新成功，模型ID: {}", getModelType(), modelId);
    }

    /**
     * 获取模型类型
     * @return 模型类型（如: "chat", "embedding"）
     */
    protected abstract String getModelType();

    /**
     * 从DTO构建更新实体
     * @param modelId 模型ID
     * @param updateData 更新数据
     * @param version 当前版本号
     * @return 更新实体
     */
    protected abstract T buildUpdatedEntity(String modelId, D updateData, Long version);

    /**
     * 从表单数据构建实体
     * @param modelId 模型ID
     * @param provider 提供商
     * @param formData 表单数据
     * @param version 当前版本号
     * @return 构建的实体
     */
    protected abstract T buildEntityFromFormData(String modelId, String provider, Map<String, Object> formData, Long version);

    /**
     * 更新缓存
     * @param modelId 模型ID
     * @param entity 实体
     */
    protected abstract void updateCache(String modelId, T entity);


    /**
     * 验证并获取当前实体
     */
    private BaseModelEntity validateAndGetCurrentEntity(String modelId) {
        BaseModelEntity entity = modelRepository.queryModelById(modelId);
        if (entity == null) {
            throw new IllegalArgumentException("模型不存在，模型ID: " + modelId);
        }
        return entity;
    }

    /**
     * 验证模型类型
     */
    private void validateModelType(BaseModelEntity currentEntity) {
        if (!getModelType().equalsIgnoreCase(currentEntity.getType())) {
            throw new IllegalArgumentException("模型类型不匹配，期望: " + getModelType() + "，实际: " + currentEntity.getType());
        }
    }

    /**
     * 校验表单数据
     */
    private ValidationResult validateFormData(String provider, String type, Map<String, Object> formData) {
        return dynamicForm.validateFormData(provider, type, formData);
    }
    
    /**
     * 应用默认值到表单数据
     */
    private Map<String, Object> applyDefaultValues(String provider, String type, Map<String, Object> formData) {
        return dynamicForm.applyDefaultValues(provider, type, formData);
    }

    /**
     * 执行更新操作
     */
    private void executeUpdate(String modelId, T updatedEntity) {
        try {
            // 1. 更新数据库
            modelRepository.updateModelRecord(updatedEntity);

            // 2. 获取最新实体
            BaseModelEntity latestEntity = modelRepository.queryModelById(modelId);

            // 3. 更新缓存
            @SuppressWarnings("unchecked")
            T typedEntity = (T) latestEntity;
            updateCache(modelId, typedEntity);

        } catch (OptimisticLockException e) {
            log.warn("{}模型配置更新失败，存在并发冲突，模型ID: {}", getModelType(), modelId);
            throw e;
        } catch (Exception e) {
            log.error("{}模型配置更新失败，模型ID: {}, 错误: {}", getModelType(), modelId, e.getMessage(), e);
            throw e;
        }
    }
}
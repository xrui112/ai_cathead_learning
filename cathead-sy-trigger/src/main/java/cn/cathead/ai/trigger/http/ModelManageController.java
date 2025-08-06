package cn.cathead.ai.trigger.http;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.domain.model.service.IModelService;

import cn.cathead.ai.types.model.Response;
import cn.cathead.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 模型管理控制器
 * 提供模型配置的增删改查功能
 * URL
 * base /api/v1/manage
 * /chat  /embedding   分别是针对chat or embedding的接口
 * /model  通用的接口
 * /model_form 动态表单的接口（模型创建统一通过表单完成）
 *
 */
@RestController
@RequestMapping("/api/v1/manage")
@Slf4j
public class ModelManageController {

    @Resource
    private IModelService modelService;

    @Resource
    private IModelCacheManager modelBeanManager;


    /**
     * 更新Chat模型配置
     * @param modelId 模型ID
     * @param provider 提供商
     * @param formData 表单数据
     * @return 操作结果
     */
    @RequestMapping(value = "chat/{modelId}",method = RequestMethod.PUT)
    public Response<String> updateChatModelConfig(@PathVariable String modelId,
                                                  @RequestParam String provider,
                                                  @RequestBody Map<String, Object> formData) {
        try {
            log.info("收到更新Chat模型配置请求，模型ID: {}, provider: {}", modelId, provider);
            modelService.updateChatModelConfigByFormData(modelId, provider, formData);
            return new Response<>(ResponseCode.SUCCESS_UPDATE_CHAT.getCode(), ResponseCode.SUCCESS_UPDATE_CHAT.getInfo(), null);
        } catch (Exception e) {
            log.error("更新Chat模型配置失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_UPDATE_CHAT.getCode(), ResponseCode.FAILED_UPDATE_CHAT.getInfo() + ": " + e.getMessage(), null);
        }
    }
    /**
     * 更新Embedding模型配置
     * @param modelId 模型ID
     * @param provider 提供商
     * @param formData 表单数据
     * @return 操作结果
     */
    @RequestMapping(value = "embedding/{modelId}", method = RequestMethod.PUT)
    public Response<String> updateEmbeddingModelConfig(@PathVariable String modelId,
                                                       @RequestParam String provider,
                                                       @RequestBody Map<String, Object> formData) {
        try {
            log.info("收到更新Embedding模型配置请求，模型ID: {}, provider: {}", modelId, provider);
            modelService.updateEmbeddingModelConfigByFormData(modelId, provider, formData);
            return new Response<>(ResponseCode.SUCCESS_UPDATE_EMBEDDING.getCode(), ResponseCode.SUCCESS_UPDATE_EMBEDDING.getInfo(), null);
        } catch (Exception e) {
            log.error("更新Embedding模型配置失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_UPDATE_EMBEDDING.getCode(), ResponseCode.FAILED_UPDATE_EMBEDDING.getInfo() + ": " + e.getMessage(), null);
        }
    }

    /**
     * 删除模型
     * @param modelId 模型ID
     * @return 操作结果
     *删除操作不存在失败,只要调用,必执行完整的删除操作,及时数据不存在
     */
    @RequestMapping(value = "model/{modelId}",method = RequestMethod.DELETE)
    public Response<String> deleteModel(@PathVariable String modelId) {
        try {
            log.info("收到删除模型请求，模型ID: {}", modelId);
            modelService.deleteModel(modelId);
            return new Response<>(ResponseCode.SUCCESS_DELETE.getCode(), ResponseCode.SUCCESS_DELETE.getInfo(), null);
        } catch (Exception e) {
            log.error("删除模型失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>(ResponseCode.SUCCESS_DELETE.getCode(), ResponseCode.SUCCESS_DELETE.getInfo() + ": " + e.getMessage(), null);
        }
    }

    /**
     * 获取模型信息
     * @param modelId 模型ID
     * @return 模型信息
     */
    @RequestMapping(value = "model/{modelId}",method = RequestMethod.GET)
    public Response<BaseModelEntity> getModelById(@PathVariable String modelId) {
        try {
            log.info("收到获取模型信息请求，模型ID: {}", modelId);
            BaseModelEntity modelEntity = modelService.getModelById(modelId);
            if (modelEntity != null) {
                return new Response<>(ResponseCode.SUCCESS_GET_MODEL.getCode(), ResponseCode.SUCCESS_GET_MODEL.getInfo(), modelEntity);
            } else {
                return new Response<>(ResponseCode.MODEL_NOT_FOUND.getCode(), ResponseCode.MODEL_NOT_FOUND.getInfo(), null);
            }
        } catch (Exception e) {
            log.error("获取模型信息失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_GET_MODEL.getCode(), ResponseCode.FAILED_GET_MODEL.getInfo() + ": " + e.getMessage(), null);
        }
    }

    /**
     * 刷新模型缓存
     * @param modelId 模型ID
     * @return 操作结果
     */
    @RequestMapping(value = "model/{modelId}/refresh",method = RequestMethod.POST)
    public Response<String> refreshModelCache(@PathVariable String modelId) {
        try {
            log.info("收到刷新模型缓存请求，模型ID: {}", modelId);
            modelService.refreshModelCache(modelId);
            return new Response<>(ResponseCode.SUCCESS_REFRESH_CACHE.getCode(), ResponseCode.SUCCESS_REFRESH_CACHE.getInfo(), null);
        } catch (Exception e) {
            log.error("刷新模型缓存失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_REFRESH_CACHE.getCode(), ResponseCode.FAILED_REFRESH_CACHE.getInfo() + ": " + e.getMessage(), null);
        }
    }

    /**
     * 批量刷新所有模型缓存
     * @return 操作结果
     */
    @RequestMapping(value = "model/refresh-all",method = RequestMethod.POST)
    public Response<String> refreshAllModelCache() {
        try {
            log.info("收到批量刷新所有模型缓存请求");
            // todo 里可以实现批量刷新的逻辑
            // 暂时返回成功，具体实现可以根据需要添加
            return new Response<>(ResponseCode.SUCCESS_REFRESH_ALL_CACHE.getCode(), ResponseCode.SUCCESS_REFRESH_ALL_CACHE.getInfo(), null);
        } catch (Exception e) {
            log.error("批量刷新模型缓存失败，错误: {}", e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_REFRESH_ALL_CACHE.getCode(), ResponseCode.FAILED_REFRESH_ALL_CACHE.getInfo() + ": " + e.getMessage(), null);
        }
    }

    /**
     * 获取Bean管理统计信息
     * @return Bean管理统计信息
     */
    @RequestMapping(value = "model/bean/stats",method = RequestMethod.GET)
    public Response<Map<String, Object>> getBeanStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("beanStats", modelBeanManager.getModelBeanStats());
            stats.put("chatModelCache", modelBeanManager.getAllChatModelCache().size());
            stats.put("embeddingModelCache", modelBeanManager.getAllEmbeddingModelCache().size());

            return new Response<>(ResponseCode.SUCCESS_GET_BEAN_STATS.getCode(), ResponseCode.SUCCESS_GET_BEAN_STATS.getInfo(), stats);
        } catch (Exception e) {
            log.error("获取Bean管理统计信息失败，错误: {}", e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_GET_BEAN_STATS.getCode(), ResponseCode.FAILED_GET_BEAN_STATS.getInfo() + ": " + e.getMessage(), null);
        }
    }

    /**
     * 清空所有模型Bean
     * @return 操作结果
     */
    @RequestMapping(value = "model/bean/clear",method = RequestMethod.POST)
    public Response<String> clearAllModelBeans() {
        try {
            log.info("收到清空所有模型Bean请求");
            modelBeanManager.clearAllModelBeans();
            return new Response<>(ResponseCode.SUCCESS_CLEAR_BEANS.getCode(), ResponseCode.SUCCESS_CLEAR_BEANS.getInfo(), null);
        } catch (Exception e) {
            log.error("清空所有模型Bean失败，错误: {}", e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_CLEAR_BEANS.getCode(), ResponseCode.FAILED_CLEAR_BEANS.getInfo() + ": " + e.getMessage(), null);
        }
    }

    /**
     * 获取动态表单配置
     * 前端首先提供Provider&type选择(供应商&chat/embedding模型)
     * 再调用此接口获取对应模型供应商配置
     *
     * @param provider 提供商 (如: ollama, openai)
     * @param type 模型类型 (如: chat, embedding)
     * @return 表单配置信息
     */
    @RequestMapping(value = "model-form/config",method = RequestMethod.GET)
    public Response<FormConfiguration> getFormConfiguration(@RequestParam String provider,
                                                            @RequestParam String type) {
        try {
            log.info("收到获取动态表单配置请求，provider: {}, type: {}", provider, type);
            FormConfiguration config = modelService.getFormConfiguration(provider, type);
            if (config != null) {
                return new Response<>(ResponseCode.SUCCESS_GET_FORM_CONFIG.getCode(), ResponseCode.SUCCESS_GET_FORM_CONFIG.getInfo(), config);
            } else {
                return new Response<>(ResponseCode.UNSUPPORTED_PROVIDER_TYPE.getCode(), ResponseCode.UNSUPPORTED_PROVIDER_TYPE.getInfo() + ": " + provider + ":" + type, null);
            }
        } catch (Exception e) {
            log.error("获取动态表单配置失败，provider: {}, type: {}, 错误: {}",
                    provider, type, e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_GET_FORM_CONFIG.getCode(), ResponseCode.FAILED_GET_FORM_CONFIG.getInfo() + ": " + e.getMessage(), null);
        }
    }


    /**
     * 提交动态表单并创建模型
     * 用户提交表格时，校验通过后调用此接口提交
     *
     * @param provider 提供商
     * @param type 模型类型
     * @param formData 表单数据
     * @return 创建结果
     */
    @RequestMapping(value = "model-form/submit",method = RequestMethod.POST)
    public Response<String> submitForm(@RequestParam("provider") String provider,
                                       @RequestParam("type") String type,
                                       @RequestBody Map<String, Object> formData) {
        try {
            log.info("收到提交动态表单请求，provider: {}, type: {}", provider, type);
            String result = modelService.submitForm(provider, type, formData);
            return new Response<>(ResponseCode.SUCCESS_SUBMIT_FORM.getCode(), result, null);
        } catch (Exception e) {
            log.error("提交动态表单失败，provider: {}, type: {}, 错误: {}",
                    provider, type, e.getMessage(), e);
            return new Response<>(ResponseCode.FAILED_SUBMIT_FORM.getCode(), ResponseCode.FAILED_SUBMIT_FORM.getInfo() + ": " + e.getMessage(), null);
        }
    }
}
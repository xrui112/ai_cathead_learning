package cn.cathead.ai.trigger.http;

import cn.cathead.ai.api.dto.ChatModelDTO;
import cn.cathead.ai.api.dto.ChatRequestDto;
import cn.cathead.ai.api.dto.EmbeddingModelDTO;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.service.ModelBean.IModelBeanManager;
import cn.cathead.ai.domain.model.service.IModelService;
import cn.cathead.ai.types.model.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

//todo responseCode and info 还没有规范
/**
 * 模型管理控制器
 * 提供模型配置的增删改查功能
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class ModelServiceController {

    @Resource
    private IModelService modelService;

    @Resource
    private IModelBeanManager modelBeanManager;

    @RequestMapping(value = "model/creat/chat",method = RequestMethod.POST)
    public void creatChat(@RequestParam ChatModelDTO chatModelDto) {
        modelService.creatModel(chatModelDto);
    }

    @RequestMapping(value = "model/creat/embedding",method = RequestMethod.POST)
    public void creatEmbedding(@RequestParam EmbeddingModelDTO embeddingModelDTO) {
        modelService.creatModel(embeddingModelDTO);
    }

    @RequestMapping(value = "model/chat_with",method = RequestMethod.GET)
    public Flux<org.springframework.ai.chat.model.ChatResponse> chatWith(@RequestParam ChatRequestDto chatRequestDto) {
        return modelService.chatWith(chatRequestDto);
    }

    /**
     * 更新Chat模型配置
     * @param modelId 模型ID
     * @param chatModelDTO 新的模型配置
     * @return 操作结果
     */
    @PutMapping("model/chat/{modelId}")
    public Response<String> updateChatModelConfig(@PathVariable String modelId, 
                                                 @RequestBody ChatModelDTO chatModelDTO) {
        try {
            log.info("收到更新Chat模型配置请求，模型ID: {}", modelId);
            modelService.updateChatModelConfig(modelId, chatModelDTO);
            return new Response<>("0000", "Chat模型配置更新成功", null);
        } catch (Exception e) {
            log.error("更新Chat模型配置失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>("0001", "更新Chat模型配置失败: " + e.getMessage(), null);
        }
    }

    /**
     * 更新Embedding模型配置
     * @param modelId 模型ID
     * @param embeddingModelDTO 新的模型配置
     * @return 操作结果
     */
    @PutMapping("model/embedding/{modelId}")
    public Response<String> updateEmbeddingModelConfig(@PathVariable String modelId, 
                                                      @RequestBody EmbeddingModelDTO embeddingModelDTO) {
        try {
            log.info("收到更新Embedding模型配置请求，模型ID: {}", modelId);
            modelService.updateEmbeddingModelConfig(modelId, embeddingModelDTO);
            return new Response<>("0000", "Embedding模型配置更新成功", null);
        } catch (Exception e) {
            log.error("更新Embedding模型配置失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>("0001", "更新Embedding模型配置失败: " + e.getMessage(), null);
        }
    }

    /**
     * 删除模型
     * @param modelId 模型ID
     * @return 操作结果
     */
    @DeleteMapping("model/{modelId}")
    public Response<String> deleteModel(@PathVariable String modelId) {
        try {
            log.info("收到删除模型请求，模型ID: {}", modelId);
            modelService.deleteModel(modelId);
            return new Response<>("0000", "模型删除成功", null);
        } catch (Exception e) {
            log.error("删除模型失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>("0001", "删除模型失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取模型信息
     * @param modelId 模型ID
     * @return 模型信息
     */
    @GetMapping("model/{modelId}")
    public Response<BaseModelEntity> getModelById(@PathVariable String modelId) {
        try {
            log.info("收到获取模型信息请求，模型ID: {}", modelId);
            BaseModelEntity modelEntity = modelService.getModelById(modelId);
            if (modelEntity != null) {
                return new Response<>("0000", "获取模型信息成功", modelEntity);
            } else {
                return new Response<>("0001", "模型不存在", null);
            }
        } catch (Exception e) {
            log.error("获取模型信息失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>("0001", "获取模型信息失败: " + e.getMessage(), null);
        }
    }

    /**
     * 刷新模型缓存
     * @param modelId 模型ID
     * @return 操作结果
     */
    @PostMapping("model/{modelId}/refresh")
    public Response<String> refreshModelCache(@PathVariable String modelId) {
        try {
            log.info("收到刷新模型缓存请求，模型ID: {}", modelId);
            modelService.refreshModelCache(modelId);
            return new Response<>("0000", "模型缓存刷新成功", null);
        } catch (Exception e) {
            log.error("刷新模型缓存失败，模型ID: {}, 错误: {}", modelId, e.getMessage(), e);
            return new Response<>("0001", "刷新模型缓存失败: " + e.getMessage(), null);
        }
    }

    /**
     * 批量刷新所有模型缓存
     * @return 操作结果
     */
    @PostMapping("model/refresh/all")
    public Response<String> refreshAllModelCache() {
        try {
            log.info("收到批量刷新所有模型缓存请求");
            // todo 里可以实现批量刷新的逻辑
            // 暂时返回成功，具体实现可以根据需要添加
            return new Response<>("0000", "批量刷新模型缓存成功", null);
        } catch (Exception e) {
            log.error("批量刷新模型缓存失败，错误: {}", e.getMessage(), e);
            return new Response<>("0001", "批量刷新模型缓存失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取Bean管理统计信息
     * @return Bean管理统计信息
     */
    @GetMapping("model/bean/stats")
    public Response<Map<String, Object>> getBeanStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("beanStats", modelBeanManager.getModelBeanStats());
            stats.put(" chatModelCache", modelBeanManager.getAllChatModelCache().size());
            stats.put("embeddingModelCache", modelBeanManager.getAllEmbeddingModelCache().size());
            
            return new Response<>("0000", "获取Bean管理统计信息成功", stats);
        } catch (Exception e) {
            log.error("获取Bean管理统计信息失败，错误: {}", e.getMessage(), e);
            return new Response<>("0001", "获取Bean管理统计信息失败: " + e.getMessage(), null);
        }
    }

    /**
     * 清空所有模型Bean
     * @return 操作结果
     */
    @PostMapping("model/bean/clear")
    public Response<String> clearAllModelBeans() {
        try {
            log.info("收到清空所有模型Bean请求");
            modelBeanManager.clearAllModelBeans();
            return new Response<>("0000", "清空所有模型Bean成功", null);
        } catch (Exception e) {
            log.error("清空所有模型Bean失败，错误: {}", e.getMessage(), e);
            return new Response<>("0001", "清空所有模型Bean失败: " + e.getMessage(), null);
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
    @GetMapping("model-form/config")
    public Response<FormConfiguration> getFormConfiguration(@RequestParam String provider, 
                                                           @RequestParam String type) {
        try {
            log.info("收到获取动态表单配置请求，provider: {}, type: {}", provider, type);
            FormConfiguration config = modelService.getFormConfiguration(provider, type);
            if (config != null) {
                return new Response<>("00004", "获取表单配置成功", config);
            } else {
                return new Response<>("0005", "不支持的提供商或类型: " + provider + ":" + type, null);
            }
        } catch (Exception e) {
            log.error("获取动态表单配置失败，provider: {}, type: {}, 错误: {}", 
                    provider, type, e.getMessage(), e);
            return new Response<>("0006", "获取表单配置失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 校验动态表单数据
     * 用户提交表格时，先通过此接口走校验逻辑
     * @param provider 提供商
     * @param type 模型类型
     * @param formData 表单数据
     * @return 校验结果
     */
    @PostMapping("model-form/validate")
    public Response<ValidationResult> validateFormData(@RequestParam String provider,
                                                      @RequestParam String type,
                                                      @RequestBody Map<String, Object> formData) {
        try {
            log.info("收到校验动态表单数据请求，provider: {}, type: {}", provider, type);
            ValidationResult result = modelService.validateFormData(provider, type, formData);
            if (result.isValid()) {
                return new Response<>("0007", "表单数据校验通过", result);
            } else {
                return new Response<>("0008", "表单数据校验失败", result);
            }
        } catch (Exception e) {
            log.error("校验动态表单数据失败，provider: {}, type: {}, 错误: {}", 
                    provider, type, e.getMessage(), e);
            return new Response<>("0008", "表单数据校验失败: " + e.getMessage(), null);
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
    @PostMapping("model-form/submit")
    public Response<String> submitForm(@RequestParam String provider,
                                      @RequestParam String type,
                                      @RequestBody Map<String, Object> formData) {
        try {
            log.info("收到提交动态表单请求，provider: {}, type: {}", provider, type);
            String result = modelService.submitForm(provider, type, formData);
            return new Response<>("0009", result, null);
        } catch (Exception e) {
            log.error("提交动态表单失败，provider: {}, type: {}, 错误: {}", 
                    provider, type, e.getMessage(), e);
            return new Response<>("0010", "提交表单失败: " + e.getMessage(), null);
        }
    }
}
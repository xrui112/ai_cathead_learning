package cn.cathead.ai.domain.model.service;


import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface IModelService {

    public String createModel(ChatModelDTO chatModelDTO);

    public String createModel(EmbeddingModelDTO embeddingModelDTO);

    public Flux<ChatResponse> chatWith(ChatRequestDTO chatRequestDto);

    public void updateChatModelConfig(String modelId, ChatModelDTO chatModelDTO);

    public void updateEmbeddingModelConfig(String modelId, EmbeddingModelDTO embeddingModelDTO);

    public void deleteModel(String modelId);

    public BaseModelEntity getModelById(String modelId);

    public void refreshModelCache(String modelId);


    public EmbeddingModel getLatestEmbeddingModel(String modelId);

    public ChatModel getLatestChatModel(String modelId);

    /**
     * 检查模型版本状态
     * @param modelId 模型ID
     * @return 版本状态信息
     */
    public String getModelVersionStatus(String modelId);

    
    /**
     * 获取动态表单配置
     * @param provider 提供商
     * @param type 模型类型
     * @return 表单配置
     */
    FormConfiguration getFormConfiguration(String provider, String type);
    
    /**
     * 校验动态表单数据
     * @param provider 提供商
     * @param type 模型类型
     * @param formData 表单数据
     * @return 校验结果
     */
    ValidationResult validateFormData(String provider, String type, Map<String, Object> formData);
    
    /**
     * 提交动态表单并创建模型
     * @param provider 提供商
     * @param type 模型类型
     * @param formData 表单数据
     * @return 创建结果信息
     */
    String submitForm(String provider, String type, Map<String, Object> formData);

}

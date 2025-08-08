package cn.cathead.ai.domain.model.service;


import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import cn.cathead.ai.types.dto.ImageChatRequestDTO;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface IModelService {



    public Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto);

    
    /**
     * 使用formData更新Chat模型配置
     * @param modelId 模型ID
     * @param provider 提供商
     * @param formData 表单数据
     */
    public void updateChatModelConfigByFormData(String modelId, String provider, Map<String, Object> formData);
    
    /**
     * 使用formData更新Embedding模型配置
     * @param modelId 模型ID  
     * @param provider 提供商
     * @param formData 表单数据
     */
    public void updateEmbeddingModelConfigByFormData(String modelId, String provider, Map<String, Object> formData);

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

    /**
     *
     * @param chatRequestDto
     * @return 普通调用chat
     */
    ChatResponse chatWith(ChatRequestDTO chatRequestDto);

    /**
     * 图片聊天接口（非流式）
     * @param imageChatRequestDto 包含图片和文字prompt的请求
     * @return ChatResponse 聊天响应
     */
    ChatResponse chatWithImage(ImageChatRequestDTO imageChatRequestDto);

    /**
     * 图片聊天接口（流式）
     * @param imageChatRequestDto 包含图片和文字prompt的请求
     * @return Flux<ChatResponse> 流式聊天响应
     */
    Flux<ChatResponse> chatWithImageStream(ImageChatRequestDTO imageChatRequestDto);


    /**
     * 图片聊天接口（返回纯文本）
     * @param imageChatRequestDto 包含图片和文字prompt的请求
     * @return String 纯文本响应
     */
    String chatWithImageText(ImageChatRequestDTO imageChatRequestDto);

    /**
     * 文本向量化接口（返回完整EmbeddingResponse）
     * @param embeddingRequestDto 向量化请求
     * @return EmbeddingResponse 完整的向量化响应
     */
    EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto);

    /**
     * 文本向量化接口（返回向量数组）
     *
     * @param embeddingRequestDto 向量化请求
     * @return List<List < Double>> 向量数组
     */
    List<float[]> embedTextVectors(EmbeddingRequestDTO embeddingRequestDto);

    /**
     * 单个文本向量化接口（返回单个向量）
     * @param embeddingRequestDto 向量化请求
     * @return List<Double> 单个向量
     */
    float[] embedSingleTextVector(EmbeddingRequestDTO embeddingRequestDto);

}

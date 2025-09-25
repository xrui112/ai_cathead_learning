package cn.cathead.ai.domain.model.service;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import reactor.core.publisher.Flux;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;


public interface IModelService {

    public Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto);

    ChatResponse chatWith(ChatRequestDTO chatRequestDto);

    EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto);


    // 表单更新相关接口已移除，统一由业务服务自行使用规则校验

    public void deleteModel(String modelId);

    public BaseModelEntity getModelById(String modelId);

    public void refreshModelCache(String modelId);


    public EmbeddingModel getLatestEmbeddingModel(String modelId);

    public ChatModel getLatestChatModel(String modelId);

    /**
     * 检查模型版本状态
     *
     * @param modelId 模型ID
     * @return 版本状态信息
     */
    public String getModelVersionStatus(String modelId);


    // 动态表单相关接口已移除


    /**
     * 统一创建Chat模型（对外门面）。
     */
    String createChatModel(ChatModelDTO chatModelDTO);

    /**
     * 统一创建Embedding模型（对外门面）。
     */
    String createEmbeddingModel(EmbeddingModelDTO embeddingModelDTO);

}

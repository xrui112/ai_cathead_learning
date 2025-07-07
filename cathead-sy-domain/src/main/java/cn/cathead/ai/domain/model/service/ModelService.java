package cn.cathead.ai.domain.model.service;
import cn.cathead.ai.api.dto.ChatModelDTO;
import cn.cathead.ai.api.dto.ChatRequestDto;
import cn.cathead.ai.api.dto.EmbeddingModelDTO;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatRequestEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.ModelBean.IModelBeanManager;
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
public class ModelService implements IModelService{

    @Resource
    private Map<String, IModelProvider> modelProviderMap = new ConcurrentHashMap<>();

    @Resource
    private IModelRepository iModelRepository;

    // 使用接口来管理模型Bean
    @Resource
    private IModelBeanManager modelBeanManager;

    @Override
    public void creatModel(ChatModelDTO chatModelDTO) {
        ChatModelEntity chatModelEntity = ChatModelEntity.builder()
                .modelId(UUID.randomUUID().toString())
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
                .build();

        // 使用ModelBeanManager创建模型Bean
        ChatModel chatModel = modelBeanManager.createChatModelBean(chatModelEntity);

        if(null==chatModel){
                log.info("生成失败 : { }");
        }else {
            log.info("生成成功 名字: { }"+chatModelEntity.getModelName());
            //存到数据库
            iModelRepository.saveModelRecord(chatModelEntity);
        }
    }

    @Override
    public void creatModel(EmbeddingModelDTO embeddingModelDTO) {
        EmbeddingModelEntity embeddingModelEntity = EmbeddingModelEntity.builder()
                .modelId(UUID.randomUUID().toString())
                .providerName(embeddingModelDTO.getProviderName())
                .modelName(embeddingModelDTO.getModelName())
                .url(embeddingModelDTO.getUrl())
                .key(embeddingModelDTO.getKey())
                .type(embeddingModelDTO.getType())
                .embeddingFormat(embeddingModelDTO.getEmbeddingFormat())
                .numPredict(embeddingModelDTO.getNumPredict())
                .build();

        // 使用ModelBeanManager创建模型Bean
        EmbeddingModel embeddingModel = modelBeanManager.createEmbeddingModelBean(embeddingModelEntity);
            if(null==embeddingModel){
                log.info("生成失败 : { }");
            }else {
                log.info("生成成功 名字: { }"+embeddingModelDTO.getModelName());
                //存到数据库
                iModelRepository.saveModelRecord(embeddingModelEntity);
            }
    }

    /**
     *  对应使用chatModel
     * @param chatRequestDto
     * @return
     */
    @Override
    public Flux<ChatResponse> chatWith(ChatRequestDto chatRequestDto) {
        ChatRequestEntity chatRequestEntity = ChatRequestEntity.builder()
                .modelId(chatRequestDto.getModelId())
                .prompt(chatRequestDto.getPrompt())
                .build();

        //先检查ModelBeanManager中是否有
        ChatModel chatModel = modelBeanManager.getChatModelBean(chatRequestDto.getModelId());

        if(null!=chatModel){
            //直接使用
            return generateStream(chatModel, chatRequestDto.getPrompt());
        }
        
        //如果ModelBeanManager中没有，从数据库加载并创建
        ChatModelEntity chatModelEntity=(ChatModelEntity) iModelRepository.queryModelById(chatRequestEntity);
        if (chatModelEntity != null) {
            chatModel = modelBeanManager.createChatModelBean(chatModelEntity);
            if (chatModel != null) {
                return generateStream(chatModel, chatRequestEntity.getPrompt());
            }
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
        
        // 4. 更新成功，刷新内存中的模型Bean
        modelBeanManager.updateChatModelBean(modelId, chatModelEntity);
        log.info("Chat模型配置更新成功，模型ID: {}", modelId);
        
    } catch (OptimisticLockException e) {
        log.warn("Chat模型配置更新失败，存在并发冲突，模型ID: {}", modelId);
        throw e; // 重新抛出异常让Controller处理
    }
    }

    @Override
    public void updateEmbeddingModelConfig(String modelId, EmbeddingModelDTO embeddingModelDTO) {
        log.info("开始更新Embedding模型配置，模型ID: {}", modelId);
        
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
                .build();
        
        // 2. 更新数据库
        iModelRepository.updateModelRecord(embeddingModelEntity);
        
        // 3. 使用ModelBeanManager更新模型Bean
        EmbeddingModel newEmbeddingModel = modelBeanManager.updateEmbeddingModelBean(modelId, embeddingModelEntity);
        
        if (newEmbeddingModel != null) {
            log.info("Embedding模型配置更新成功，模型ID: {}", modelId);
        } else {
            log.error("Embedding模型配置更新失败，无法创建新模型，模型ID: {}", modelId);
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

    @Override
    public void refreshModelCache(String modelId) {
        log.info("开始刷新模型Bean，模型ID: {}", modelId);
        
        // 1. 从数据库重新加载模型信息
        BaseModelEntity modelEntity = iModelRepository.queryModelById(modelId);
        if (modelEntity == null) {
            log.warn("模型不存在，无法刷新Bean，模型ID: {}", modelId);
            return;
        }
        
        // 2. 使用ModelBeanManager重新创建模型Bean
        if ("chat".equalsIgnoreCase(modelEntity.getType())) {
            ChatModel chatModel = modelBeanManager.updateChatModelBean(modelId, (ChatModelEntity) modelEntity);
            if (chatModel != null) {
                log.info("Chat模型Bean刷新成功，模型ID: {}", modelId);
            }
        } else {
            EmbeddingModel embeddingModel = modelBeanManager.updateEmbeddingModelBean(modelId, (EmbeddingModelEntity) modelEntity);
            if (embeddingModel != null) {
                log.info("Embedding模型Bean刷新成功，模型ID: {}", modelId);
            }
        }
    }
}

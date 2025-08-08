package cn.cathead.ai.domain.model.service;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.EmbeddingModelEntity;
import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.FormConfiguration;
import cn.cathead.ai.domain.model.model.entity.ValidationResult;
import cn.cathead.ai.domain.model.repository.IModelRepository;
import cn.cathead.ai.domain.model.service.form.IDynamicForm;
import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.domain.model.service.modelcreation.IModelCreationService;
import cn.cathead.ai.domain.model.service.provider.IModelProvider;
import cn.cathead.ai.domain.model.service.update.impl.ChatModelUpdateService;
import cn.cathead.ai.domain.model.service.update.impl.EmbeddingModelUpdateService;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ModelService implements IModelService {

    @Resource
    private Map<String, IModelProvider> modelProviderMap = new ConcurrentHashMap<>();

    @Resource
    private IModelRepository iModelRepository;

    // 使用接口来管理模型Bean
    @Resource
    private IModelCacheManager modelBeanManager;

    // 动态表单服务
    @Resource
    private IDynamicForm dynamicForm;
    
    // 模型创建服务
    @Resource
    private IModelCreationService modelCreationService;
    
    // 模型更新服务
    @Resource
    private ChatModelUpdateService chatModelUpdateService;
    
    @Resource
    private EmbeddingModelUpdateService embeddingModelUpdateService;


    /**
     *  对应使用chatModel
     * @param chatRequestDto
     * @return
     *
     * 需要注意的是,调用该接口之前,已经默认已经createModel 处理过了
     *
     */
    @Override
    public Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto) {
        // !!!!!!先检查并确保缓存是最新版本 所有的model使用 都要先ensureLatestChatModel检查version
        ChatModel chatModel = ensureLatestChatModel(chatRequestDto.getModelId());
        if(null==chatModel){
            log.error("未找到模型，模型ID: {}", chatRequestDto.getModelId());
            return Flux.empty();
        }

        return generateStream(chatModel,chatRequestDto);

    }
    public Flux<ChatResponse> generateStream(ChatModel chatModel,ChatRequestDTO chatRequestDto) {
        log.info("调用流式接口");
        boolean withImage = Boolean.TRUE.equals(chatRequestDto.getWithImage());
        String message= chatRequestDto.getPrompt();
        if (!withImage){
            return chatModel.stream(
                    new Prompt(
                            message
                    )
            );
        }
        return  generateStreamWithImage(chatModel,chatRequestDto);
    }
    private Flux<ChatResponse> generateStreamWithImage(ChatModel chatModel, ChatRequestDTO request) {
        try {
            log.info("调用带图片的流式聊天接口");

            // 处理图片
            byte[] imageBytes = request.getImage();
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("图片文件为空，降级为普通文本流式聊天");
                throw new Exception("图片为空");
            }

            // 创建 Resource 和 Media 对象
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes);
            Media media = new Media(
                    MimeTypeUtils.IMAGE_JPEG,
                    imageResource
            );
            log.info(String.valueOf(media));

            String defaultPrompt = "请分析这张图片";
            UserMessage userMessage = UserMessage.builder()
                    .text(request.getPrompt() != null && !request.getPrompt().trim().isEmpty()
                            ? request.getPrompt()
                            : defaultPrompt)
                    .media(media)
                    .build();

            Prompt prompt = new Prompt(userMessage);

            return chatModel.stream(prompt);

        } catch (Exception e) {
            log.error("图片流式聊天处理失败", e);
            return Flux.error(new AppException(ResponseCode.FAILED_CHAT.getCode(), "图片流式聊天处理失败: " + e.getMessage()));
        }
    }

    @Override
    public ChatResponse chatWith(ChatRequestDTO chatRequestDto) {
        // !!!!!!先检查并确保缓存是最新版本 所有的model使用 都要先ensureLatestChatModel检查version
        ChatModel chatModel = ensureLatestChatModel(chatRequestDto.getModelId());

        if (chatModel != null) {
            return generate(chatModel, chatRequestDto);
        }else {
            throw new AppException(ResponseCode.FAILED_CHAT.getCode(),ResponseCode.FAILED_CHAT.getInfo());
        }

    }
    public ChatResponse generate(ChatModel chatModel,ChatRequestDTO chatRequestDTO) {
        log.info("调用普通聊天接口");
        Boolean withImage = Boolean.TRUE.equals(chatRequestDTO.getWithImage());
        String message= chatRequestDTO.getPrompt();
        if (!withImage){
            return chatModel.call(
                    new Prompt(
                            message
                    )
            );
        }
        return  generateWithImage(chatModel,chatRequestDTO);
    }

    private ChatResponse generateWithImage(ChatModel chatModel, ChatRequestDTO request) {
        try {
            log.info("调用带图片的流式聊天接口");

            // 处理图片
            byte[] imageBytes = request.getImage();
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("图片文件为空，降级为普通文本流式聊天");
                throw new Exception("图片为空");
            }

            // 创建 Resource 和 Media 对象
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes);
            Media media = new Media(
                    MimeTypeUtils.IMAGE_JPEG,
                    imageResource
            );
            log.info(String.valueOf(media));
            String defaultPrompt = "请分析这张图片";
            UserMessage userMessage = UserMessage.builder()
                    .text(request.getPrompt() != null && !request.getPrompt().trim().isEmpty()
                            ? request.getPrompt()
                            : defaultPrompt)
                    .media(media)
                    .build();

            Prompt prompt = new Prompt(userMessage);
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("图片聊天处理失败", e);
            throw new AppException(ResponseCode.FAILED_CHAT.getCode(), "图片聊天处理失败: " + e.getMessage());
        }
    }

    @Override
    public EmbeddingResponse embedText(EmbeddingRequestDTO embeddingRequestDto) {
        log.info("调用文本向量化接口，模型ID: {}", embeddingRequestDto.getModelId());

        // 确保获取最新的EmbeddingModel
        EmbeddingModel embeddingModel = ensureLatestEmbeddingModel(embeddingRequestDto.getModelId());

        if (embeddingModel != null) {
            return generateEmbedding(embeddingModel, embeddingRequestDto);
        } else {
            throw new AppException(ResponseCode.FAILED_EMBEDDING.getCode(), "获取Embedding模型失败");
        }
    }

    /**
     * 生成文本向量化
     */
    private EmbeddingResponse generateEmbedding(EmbeddingModel embeddingModel, EmbeddingRequestDTO request) {
        try {
            log.info("调用文本向量化处理");
            List<String> textsToEmbed = new ArrayList<>();
            // 处理输入文本
            if (request.getText() != null && !request.getText().trim().isEmpty()) {
                textsToEmbed.add(request.getText());
            }

            if (request.getTexts() != null && !request.getTexts().isEmpty()) {
                textsToEmbed.addAll(request.getTexts());
            }

            if (textsToEmbed.isEmpty()) {
                throw new AppException(ResponseCode.FAILED_EMBEDDING.getCode(), "没有提供要向量化的文本");
            }
            // 创建EmbeddingRequest，使用模型创建时已配置的维度信息
            EmbeddingRequest embeddingRequest = new EmbeddingRequest(textsToEmbed, null);
            return embeddingModel.call(embeddingRequest);
        } catch (Exception e) {
            log.error("文本向量化处理失败", e);
            throw new AppException(ResponseCode.FAILED_EMBEDDING.getCode(), "文本向量化处理失败: " + e.getMessage());
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

    public EmbeddingModel getLatestEmbeddingModel(String modelId) {
        return ensureLatestEmbeddingModel(modelId);
    }

    public ChatModel getLatestChatModel(String modelId) {
        return ensureLatestChatModel(modelId);
    }

    public String getModelVersionStatus(String modelId) {
        BaseModelEntity dbEntity = iModelRepository.queryModelById(modelId);
        if (dbEntity == null) {
            return String.format("模型[%s]不存在", modelId);
        }
        
        Long cachedVersion = modelBeanManager.getCachedModelVersion(modelId);
        Long dbVersion = dbEntity.getVersion();
        
        if (cachedVersion == null) {
            return String.format("模型[%s]：缓存中不存在，数据库版本: %d", modelId, dbVersion);
        }
        
        if (cachedVersion.equals(dbVersion)) {
            return String.format("模型[%s]：缓存版本与数据库版本一致，版本: %d", modelId, dbVersion);
        } else {
            return String.format("模型[%s]：缓存版本过期，缓存版本: %d，数据库版本: %d", 
                    modelId, cachedVersion, dbVersion);
        }
    }

    private ChatModel ensureLatestChatModel(String modelId) {
        log.debug("检查Chat模型版本，模型ID: {}", modelId);
        // 1. 从数据库获取当前版本
        ChatModelEntity currentEntity = (ChatModelEntity) iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            log.warn("模型不存在，清理缓存，模型ID: {}", modelId);
            modelBeanManager.removeChatModelBean(modelId);
            return null;
        }
        // 2. 获取缓存版本信息
        Long cachedVersion = modelBeanManager.getCachedModelVersion(modelId);
        // 3. 判断是否需要更新缓存
        if (cachedVersion == null) {
            // 3.1缓存中没有，直接创建
            log.debug("缓存中没有模型，创建新模型，模型ID: {}", modelId);
            return modelBeanManager.updateChatModelBean(modelId, currentEntity);
        } else if (cachedVersion.equals(currentEntity.getVersion())) {
            // 3.2版本一致，直接返回
            log.debug("缓存版本是最新的，直接返回缓存模型，模型ID: {}", modelId);
            return modelBeanManager.getChatModelBean(modelId);
        } else {
            // 3.3版本过期，需要更新
            log.info("缓存版本过期，更新Chat模型缓存，模型ID: {}, 数据库版本: {}, 缓存版本: {}",
                    modelId, currentEntity.getVersion(), cachedVersion);
            return modelBeanManager.updateChatModelBean(modelId, currentEntity);
        }
    }


    private EmbeddingModel ensureLatestEmbeddingModel(String modelId) {
        log.debug("检查Embedding模型版本，模型ID: {}", modelId);

        // 1. 从数据库获取当前版本
        EmbeddingModelEntity currentEntity = (EmbeddingModelEntity) iModelRepository.queryModelById(modelId);
        if (currentEntity == null) {
            log.warn("模型不存在，清理缓存，模型ID: {}", modelId);
            modelBeanManager.removeEmbeddingModelBean(modelId);
            return null;
        }

        // 2. 获取缓存版本信息
        Long cachedVersion = modelBeanManager.getCachedModelVersion(modelId);

        // 3. 判断是否需要更新缓存
        if (cachedVersion == null) {
            // 缓存中没有，直接创建
            log.debug("缓存中没有模型，创建新模型，模型ID: {}", modelId);
            return modelBeanManager.updateEmbeddingModelBean(modelId, currentEntity);
        } else if (cachedVersion.equals(currentEntity.getVersion())) {
            // 版本一致，直接返回
            log.debug("缓存版本是最新的，直接返回缓存模型，模型ID: {}", modelId);
            return modelBeanManager.getEmbeddingModelBean(modelId);
        } else {
            // 版本过期，需要更新
            log.info("缓存版本过期，更新Embedding模型缓存，模型ID: {}, 数据库版本: {}, 缓存版本: {}",
                    modelId, currentEntity.getVersion(), cachedVersion);
            return modelBeanManager.updateEmbeddingModelBean(modelId, currentEntity);
        }
    }

    @Override
    public void refreshModelCache(String modelId) {
        log.info("开始强制刷新模型Bean，模型ID: {}", modelId);
        
        // 1. 从数据库重新加载模型信息
        BaseModelEntity modelEntity = iModelRepository.queryModelById(modelId);
        if (modelEntity == null) {
            log.warn("模型不存在，无法刷新Bean，模型ID: {}", modelId);
            throw new AppException("模型不存在，无法刷新Bean，模型ID: {}", modelId);
        }
        
        // 2. 强制更新缓存，不进行版本检查
        log.info("强制刷新模型缓存，模型ID: {}, 数据库版本: {}, 原缓存版本: {}", 
                modelId, modelEntity.getVersion(), modelBeanManager.getCachedModelVersion(modelId));
        
        // 3. 使用ModelBeanManager重新创建模型Bean
        if ("chat".equalsIgnoreCase(modelEntity.getType())) {
            ChatModel chatModel = modelBeanManager.updateChatModelBean(modelId, (ChatModelEntity) modelEntity);
            if (chatModel != null) {
                log.info("Chat模型Bean强制刷新成功，模型ID: {}, 新版本: {}", 
                        modelId, modelEntity.getVersion());
            }
        } else if ("embedding".equalsIgnoreCase(modelEntity.getType())) {
            EmbeddingModel embeddingModel = modelBeanManager.updateEmbeddingModelBean(modelId, (EmbeddingModelEntity) modelEntity);
            if (embeddingModel != null) {
                log.info("Embedding模型Bean强制刷新成功，模型ID: {}, 新版本: {}", 
                        modelId, modelEntity.getVersion());
            }
        } else {
            log.warn("未知的模型类型，无法刷新，模型ID: {}, 类型: {}", modelId, modelEntity.getType());
        }
    }

    @Override
    public FormConfiguration getFormConfiguration(String provider, String type) {
        log.info("获取动态表单配置，provider: {}, type: {}", provider, type);
        return dynamicForm.getFormConfiguration(provider, type);
    }
    
    @Override
    public ValidationResult validateFormData(String provider, String type, Map<String, Object> formData) {
        log.info("校验动态表单数据，provider: {}, type: {}", provider, type);
        return dynamicForm.validateFormData(provider, type, formData);
    }
    
    @Override
    public String submitForm(String provider, String type, Map<String, Object> formData) {
        log.info("提交动态表单，provider: {}, type: {}", provider, type);
        return dynamicForm.submitForm(provider, type, formData);
    }



    @Override
    public void updateChatModelConfigByFormData(String modelId, String provider, Map<String, Object> formData) {
        chatModelUpdateService.updateModelByFormData(modelId, provider, formData);
    }

    @Override
    public void updateEmbeddingModelConfigByFormData(String modelId, String provider, Map<String, Object> formData) {
        embeddingModelUpdateService.updateModelByFormData(modelId, provider, formData);
    }
}

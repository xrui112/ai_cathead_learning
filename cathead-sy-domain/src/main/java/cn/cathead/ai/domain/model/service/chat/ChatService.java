package cn.cathead.ai.domain.model.service.chat;

import cn.cathead.ai.domain.model.service.base.AbstractModelService;
import cn.cathead.ai.domain.model.service.modelcache.IModelCacheManager;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;


@Service
@Slf4j
public class ChatService extends AbstractModelService<ChatModel> implements IChatService {

    public ChatService(IModelCacheManager modelCacheManager) {
        super(modelCacheManager, "Chat");
    }

    @Override
    public Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto) {
        ChatModel chatModel = getAndValidateModel(chatRequestDto.getModelId());
        return generateStream(chatModel, chatRequestDto);
    }

    @Override
    public ChatResponse chatWith(ChatRequestDTO chatRequestDto) {
        ChatModel chatModel = getAndValidateModel(chatRequestDto.getModelId());
        return generate(chatModel, chatRequestDto);
    }

    @Override
    public Flux<ChatResponse> generateStream(ChatModel chatModel, ChatRequestDTO chatRequestDto) {
        log.info("调用流式接口");
        byte[] image = chatRequestDto.getImage();
        boolean withImage = !(image == null || image.length == 0);
        String message = chatRequestDto.getPrompt();
        if (!withImage) {
            return chatModel.stream(new Prompt(message));
        }
        return generateStreamWithImage(chatModel, chatRequestDto);
    }

    @Override
    public ChatResponse generate(ChatModel chatModel, ChatRequestDTO chatRequestDTO) {
        log.info("调用普通聊天接口");
        byte[] image = chatRequestDTO.getImage();
        boolean withImage = !(image == null || image.length == 0);
        String message = chatRequestDTO.getPrompt();
        if (!withImage) {
            return chatModel.call(new Prompt(message));
        }
        return generateWithImage(chatModel, chatRequestDTO);
    }

    private Flux<ChatResponse> generateStreamWithImage(ChatModel chatModel, ChatRequestDTO request) {
        try {
            log.info("调用带图片的流式聊天接口");
            Prompt prompt = buildPrompt(request);
            return chatModel.stream(prompt);
        } catch (Exception e) {
            return Flux.error(handleModelCallException("图片流式聊天", e));
        }
    }

    private ChatResponse generateWithImage(ChatModel chatModel, ChatRequestDTO request) {
        try {
            log.info("调用带图片的聊天接口");
            Prompt prompt = buildPrompt(request);
            return chatModel.call(prompt);
        } catch (Exception e) {
            throw handleModelCallException("图片聊天", e);
        }
    }

    private Prompt buildPrompt(ChatRequestDTO request) throws Exception {
        byte[] imageBytes = request.getImage();
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("图片文件为空，降级为普通文本聊天");
            throw new Exception("图片为空");
        }

        ByteArrayResource imageResource = new ByteArrayResource(imageBytes);
        Media media = new Media(MimeTypeUtils.IMAGE_JPEG, imageResource);

        String defaultPrompt = "请分析这张图片";
        UserMessage userMessage = UserMessage.builder()
                .text(request.getPrompt() != null && !request.getPrompt().trim().isEmpty()
                        ? request.getPrompt()
                        : defaultPrompt)
                .media(media)
                .build();

        return Prompt.builder()
                .messages(userMessage)
                .build();
    }

    // 实现抽象方法
    @Override
    protected ChatModel getModelFromCache(String modelId) {
        return modelCacheManager.ensureLatestChatModel(modelId);
    }

    @Override
    protected AppException createModelCallException(String message) {
        return new AppException(ResponseCode.FAILED_CHAT.getCode(), message);
    }
}
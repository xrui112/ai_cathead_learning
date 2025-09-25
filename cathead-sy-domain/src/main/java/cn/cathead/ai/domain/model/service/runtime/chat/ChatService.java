package cn.cathead.ai.domain.model.service.runtime.chat;

import cn.cathead.ai.domain.model.service.registry.IModelProviderService;
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
public class ChatService implements IChatService {

    private final IModelProviderService modelProviderService;

    public ChatService(IModelProviderService modelProviderService) {
        this.modelProviderService = modelProviderService;
    }

    @Override
    public Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto) {
        log.info("调用Chat流式接口，模型ID: {}", chatRequestDto.getModelId());
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(chatRequestDto.getModelId());
        return generateStream(chatModel, chatRequestDto);
    }

    @Override
    public ChatResponse chatWith(ChatRequestDTO chatRequestDto) {
        log.info("调用Chat普通接口，模型ID: {}", chatRequestDto.getModelId());
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(chatRequestDto.getModelId());
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
            String errorMsg = String.format("图片流式聊天处理失败: %s", e.getMessage());
            log.error(errorMsg, e);
            return Flux.error(new AppException(ResponseCode.FAILED_CHAT.getCode(), errorMsg));
        }
    }

    private ChatResponse generateWithImage(ChatModel chatModel, ChatRequestDTO request) {
        try {
            log.info("调用带图片的聊天接口");
            Prompt prompt = buildPrompt(request);
            return chatModel.call(prompt);
        } catch (Exception e) {
            String errorMsg = String.format("图片聊天处理失败: %s", e.getMessage());
            log.error(errorMsg, e);
            throw new AppException(ResponseCode.FAILED_CHAT.getCode(), errorMsg);
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

}
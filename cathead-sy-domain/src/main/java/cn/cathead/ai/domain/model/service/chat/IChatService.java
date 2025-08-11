package cn.cathead.ai.domain.model.service.chat;

import cn.cathead.ai.types.dto.ChatRequestDTO;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * Chat子领域服务接口
 * 负责处理聊天相关的业务逻辑
 */
public interface IChatService {


    Flux<ChatResponse> chatWithStream(ChatRequestDTO chatRequestDto);

    ChatResponse chatWith(ChatRequestDTO chatRequestDto);

    Flux<ChatResponse> generateStream(ChatModel chatModel, ChatRequestDTO chatRequestDto);

    ChatResponse generate(ChatModel chatModel, ChatRequestDTO chatRequestDto);
}

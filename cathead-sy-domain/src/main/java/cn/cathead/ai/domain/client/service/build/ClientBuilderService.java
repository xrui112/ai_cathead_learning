package cn.cathead.ai.domain.client.service.build;

import cn.cathead.ai.domain.client.service.advisor.memory.MemoryAdvisor;
import cn.cathead.ai.domain.model.service.registry.IModelProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * 默认实现：通过模型领域获取 ChatModel，构建 ChatClient，并注册记忆 Advisor。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientBuilderService implements IClientBuilderService {

    private final IModelProviderService modelProviderService;
    private final MemoryAdvisor memoryAdvisor;

    @Override
    public ChatClient build(String modelId) {
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(modelId);
        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(memoryAdvisor)
                .build();
        log.debug("ChatClient 构建完成，模型ID: {}，已启用记忆Advisor", modelId);
        return client;
    }
}



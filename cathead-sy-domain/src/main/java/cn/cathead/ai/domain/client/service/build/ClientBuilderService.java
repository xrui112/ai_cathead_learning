package cn.cathead.ai.domain.client.service.build;

import cn.cathead.ai.domain.client.service.advisor.memory.MemoryAdvisor;
import cn.cathead.ai.domain.client.service.mcp.tools.McpAdvisorRegistry;
import cn.cathead.ai.domain.model.service.registry.IModelProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
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
    private final McpAdvisorRegistry mcpAdvisorRegistry;

    @Override
    public ChatClient build(String modelId) {
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(modelId);
        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(memoryAdvisor)
                .build();
        log.debug("ChatClient 构建完成，模型ID: {}，已启用记忆Advisor", modelId);
        return client;
    }

    @Override
    public ChatClient build(String modelId, String agentId) {
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(modelId);
        var extraAdvisors = mcpAdvisorRegistry.resolveAdvisorsForAgent(agentId);
        Advisor[] advisors;
        if (extraAdvisors == null || extraAdvisors.isEmpty()) {
            advisors = new Advisor[]{ memoryAdvisor };
        } else {
            advisors = new Advisor[extraAdvisors.size() + 1];
            advisors[0] = memoryAdvisor;
            for (int i = 0; i < extraAdvisors.size(); i++) advisors[i + 1] = extraAdvisors.get(i);
        }
        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(advisors)
                .build();
        log.debug("ChatClient 构建完成，模型ID: {}，AgentID: {}，附加工具Advisor数量: {}", modelId, agentId, extraAdvisors == null ? 0 : extraAdvisors.size());
        return client;
    }
}



package cn.cathead.ai.domain.client.service.build;

import org.springframework.ai.chat.client.ChatClient;

/**
 * Client 构建服务接口。
 * 通过模型领域提供的获取方法拿到 ChatModel，并构建带记忆 Advisor 的 ChatClient。
 */
public interface IClientBuilderService {

    /**
     * 根据模型ID构建 ChatClient，并挂载记忆相关的 Advisor。
     * @param modelId 模型ID
     * @return 已组装的 ChatClient
     */
    ChatClient build(String modelId);

    /**
     * 根据模型ID与 AgentId 构建 ChatClient，并可按 Agent 注入工具/Advisors。
     * 默认回退到仅按模型构建。
     */
    default ChatClient build(String modelId, String agentId) {
        return build(modelId);
    }
}



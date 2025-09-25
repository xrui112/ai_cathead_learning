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
}



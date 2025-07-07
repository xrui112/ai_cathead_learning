package cn.cathead.ai.domain.mcp.service;

import jakarta.annotation.Resource;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;

public class McpService {

    @Resource
    private OllamaChatModel ollamaChatModel;

    @Resource
    private ToolCallbackProvider tools;

    //

}

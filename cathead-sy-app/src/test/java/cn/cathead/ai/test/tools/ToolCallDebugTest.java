package cn.cathead.ai.test.tools;

import cn.cathead.ai.Application;
import cn.cathead.ai.domain.model.service.registry.IModelProviderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * 工具调用深度调试测试
 */
@Slf4j
@SpringBootTest(classes = Application.class)
@ActiveProfiles("dev")
@DisplayName("工具调用深度调试")
public class ToolCallDebugTest {

    @Autowired
    private IModelProviderService modelProviderService;

    @Autowired
    private List<ToolCallback> toolCallbacks;

    private static final String TEST_MODEL_ID = "741f040a-330c-42c4-a7bc-dbdd36010f8a";

    @Test
    @DisplayName("调试1: 使用 options 明确指定 tool_choice")
    public void testWithToolChoice() {
        log.info("========== 调试1: 使用 tool_choice ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        ChatClient client = ChatClient.builder(chatModel).build();
        
        String prompt = "Call text_to_sql tool with sql: SELECT * FROM users";
        
        try {
            // 明确指定 tool_choice
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .toolChoice("auto")  // 或使用 "required" 强制调用工具
                    .build();
            
            log.info("Options: {}", options);
            log.info("Prompt: {}", prompt);
            log.info("ToolCallbacks: {}", toolCallbacks.size());
            
            ChatResponse response = client.prompt(prompt)
                    .options(options)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .chatResponse();
            
            var generation = response.getResults().get(0);
            boolean hasToolCalls = generation.getOutput().getToolCalls() != null 
                && !generation.getOutput().getToolCalls().isEmpty();
            
            log.info("结果:");
            log.info("  - hasToolCalls: {}", hasToolCalls);
            log.info("  - finishReason: {}", generation.getMetadata().getFinishReason());
            log.info("  - content: {}", generation.getOutput().getText());
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }

    @Test
    @DisplayName("调试2: 强制要求调用工具 (required)")
    public void testWithToolChoiceRequired() {
        log.info("========== 调试2: 强制 tool_choice=required ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        ChatClient client = ChatClient.builder(chatModel).build();
        
        String prompt = "Check this SQL: SELECT * FROM users";
        
        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .toolChoice("required")  // 强制必须调用工具
                    .build();
            
            log.info("使用 tool_choice=required 强制调用工具");
            
            ChatResponse response = client.prompt(prompt)
                    .options(options)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .chatResponse();
            
            var generation = response.getResults().get(0);
            boolean hasToolCalls = generation.getOutput().getToolCalls() != null 
                && !generation.getOutput().getToolCalls().isEmpty();
            
            log.info("结果:");
            log.info("  - hasToolCalls: {}", hasToolCalls);
            log.info("  - finishReason: {}", generation.getMetadata().getFinishReason());
            
            if (hasToolCalls) {
                log.info("✅ 成功！工具被调用了！");
                generation.getOutput().getToolCalls().forEach(tc -> {
                    log.info("  工具: {}", tc.name());
                    log.info("  参数: {}", tc.arguments());
                });
            } else {
                log.error("❌ 即使使用 required，工具仍未被调用");
                log.error("这说明模型或 API 不支持 Function Calling");
            }
            
        } catch (Exception e) {
            log.error("调用失败 - 可能模型不支持 tool_choice=required", e);
            log.error("错误信息: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("调试3: 检查模型默认配置")
    public void testModelDefaultOptions() {
        log.info("========== 调试3: 检查模型默认配置 ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        
        log.info("ChatModel 类型: {}", chatModel.getClass().getName());
        
        var defaultOptions = chatModel.getDefaultOptions();
        log.info("默认配置: {}", defaultOptions);
        
        if (defaultOptions instanceof OpenAiChatOptions) {
            OpenAiChatOptions openAiOptions = (OpenAiChatOptions) defaultOptions;
            log.info("OpenAI 配置详情:");
            log.info("  - Model: {}", openAiOptions.getModel());
            log.info("  - Temperature: {}", openAiOptions.getTemperature());
            log.info("  - Tool Choice: {}", openAiOptions.getToolChoice());
            log.info("  - Parallel Tool Calls: {}", openAiOptions.getParallelToolCalls());
        }
    }

    @Test
    @DisplayName("调试4: 使用 Prompt 对象直接调用")
    public void testWithPromptObject() {
        log.info("========== 调试4: 使用 Prompt 对象 ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .toolChoice("auto")
                .parallelToolCalls(true)
                .build();
        
        Prompt prompt = new Prompt("Validate this SQL: SELECT * FROM users", options);
        
        log.info("Prompt: {}", prompt.getInstructions());
        log.info("Options: {}", prompt.getOptions());
        
        try {
            // 注意：直接使用 ChatModel 调用时，需要手动处理工具
            ChatResponse response = chatModel.call(prompt);
            
            var generation = response.getResults().get(0);
            boolean hasToolCalls = generation.getOutput().getToolCalls() != null 
                && !generation.getOutput().getToolCalls().isEmpty();
            
            log.info("结果:");
            log.info("  - hasToolCalls: {}", hasToolCalls);
            log.info("  - content: {}", generation.getOutput().getText());
            
        } catch (Exception e) {
            log.error("调用失败", e);
        }
    }

    @Test
    @DisplayName("调试5: 检查 ToolCallback 详细信息")
    public void testToolCallbackDetails() {
        log.info("========== 调试5: ToolCallback 详细信息 ==========");
        
        for (int i = 0; i < toolCallbacks.size(); i++) {
            ToolCallback callback = toolCallbacks.get(i);
            log.info("\n工具 #{}:", i + 1);
            log.info("  类名: {}", callback.getClass().getName());
            log.info("  toString: {}", callback.toString());
            
            // 尝试反射获取更多信息
            try {
                var fields = callback.getClass().getDeclaredFields();
                for (var field : fields) {
                    field.setAccessible(true);
                    Object value = field.get(callback);
                    log.info("  字段 {}: {}", field.getName(), value);
                }
            } catch (Exception e) {
                log.warn("  无法获取字段信息: {}", e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("调试6: 英文 Prompt 测试")
    public void testEnglishPrompt() {
        log.info("========== 调试6: 英文 Prompt ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        ChatClient client = ChatClient.builder(chatModel).build();
        
        // 使用非常明确的英文指令
        String prompt = "You MUST use the text_to_sql tool to validate the following SQL query: SELECT * FROM users";
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .toolChoice("auto")
                .build();
        
        try {
            ChatResponse response = client.prompt(prompt)
                    .options(options)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .chatResponse();
            
            var generation = response.getResults().get(0);
            boolean hasToolCalls = generation.getOutput().getToolCalls() != null 
                && !generation.getOutput().getToolCalls().isEmpty();
            
            log.info("hasToolCalls: {}", hasToolCalls);
            log.info("content: {}", generation.getOutput().getText());
            
        } catch (Exception e) {
            log.error("失败", e);
        }
    }
}


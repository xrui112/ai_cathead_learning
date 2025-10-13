package cn.cathead.ai.test.tools;

import cn.cathead.ai.Application;
import cn.cathead.ai.domain.exec.service.tools.AnalyticsTools;
import cn.cathead.ai.domain.model.service.registry.IModelProviderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具调用功能测试
 */
@Slf4j
@SpringBootTest(classes = Application.class)
@ActiveProfiles("dev")
@DisplayName("工具调用功能测试")
public class ToolCallbackTest {

    @Autowired
    private IModelProviderService modelProviderService;

    @Autowired
    private List<ToolCallback> toolCallbacks;

    @Autowired
    private AnalyticsTools analyticsTools;

    private static final String TEST_MODEL_ID = "741f040a-330c-42c4-a7bc-dbdd36010f8a";

    @Test
    @DisplayName("测试1: 验证工具是否正确注册")
    public void testToolCallbacksRegistration() {
        log.info("========== 测试1: 验证工具注册 ==========");
        
        assertNotNull(toolCallbacks, "toolCallbacks 不应为 null");
        assertFalse(toolCallbacks.isEmpty(), "toolCallbacks 不应为空");
        
        log.info("✅ 工具注册成功，共 {} 个工具", toolCallbacks.size());
        
        for (int i = 0; i < toolCallbacks.size(); i++) {
            ToolCallback callback = toolCallbacks.get(i);
            log.info("工具 #{}: {}", i + 1, callback.getClass().getName());
            log.info("  - toString: {}", callback.toString());
        }
        
        assertEquals(4, toolCallbacks.size(), "应该注册了4个工具");
    }

    @Test
    @DisplayName("测试2: 直接调用工具方法")
    public void testDirectToolCall() {
        log.info("========== 测试2: 直接调用工具 ==========");
        
        // 测试 text_to_sql（现在使用直接参数）
        var sqlResult = analyticsTools.textToSql("SELECT * FROM model");
        log.info("✅ text_to_sql 调用成功: {}", sqlResult);
        assertNotNull(sqlResult);
        assertTrue(sqlResult.containsKey("sql"));
        
        // 测试 sql_query
        try {
            var queryResult = analyticsTools.sqlQuery("mysqlDataSource", "SELECT COUNT(*) as cnt FROM model LIMIT 1");
            log.info("✅ sql_query 调用成功: {}", queryResult);
        } catch (Exception e) {
            log.warn("sql_query 调用失败（可能数据源不存在）: {}", e.getMessage());
        }
        
        log.info("工具方法本身可以正常调用");
    }

    @Test
    @DisplayName("测试3: 使用 ChatClient 简单工具调用")
    public void testSimpleToolCall() {
        log.info("========== 测试3: ChatClient 简单工具调用 ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        ChatClient client = ChatClient.builder(chatModel).build();
        
        String prompt = "Validate this SQL: SELECT * FROM users";
        
        log.info("发送 Prompt: {}", prompt);
        log.info("注册的工具数量: {}", toolCallbacks.size());
        
        try {
            // 强制调用工具
            org.springframework.ai.openai.OpenAiChatOptions options = 
                org.springframework.ai.openai.OpenAiChatOptions.builder()
                    .toolChoice("required")  // 强制必须调用工具
                    .build();
            
            ChatResponse response = client.prompt(prompt)
                    .options(options)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .chatResponse();
            
            assertNotNull(response, "响应不应为 null");
            assertFalse(response.getResults().isEmpty(), "结果不应为空");
            
            var generation = response.getResults().get(0);
            String content = generation.getOutput().getText();
            
            log.info("========== 响应分析 ==========");
            log.info("FinishReason: {}", generation.getMetadata().getFinishReason());
            log.info("响应内容: {}", content);
            
            // 检查工具调用
            boolean hasToolCalls = generation.getOutput().getToolCalls() != null 
                && !generation.getOutput().getToolCalls().isEmpty();
            
            log.info("是否触发工具调用: {}", hasToolCalls);
            
            if (hasToolCalls) {
                log.info("✅ 工具调用成功！");
                log.info("工具调用数量: {}", generation.getOutput().getToolCalls().size());
                generation.getOutput().getToolCalls().forEach(tc -> {
                    log.info("  - 工具: {} -> {}", tc.name(), tc.arguments());
                });
            } else {
                log.warn("❌ 工具未被调用！");
                log.warn("返回内容: {}", content);
                
                // 检查是否是代码块格式
                if (content.contains("```tool_code") || content.contains("tool_code")) {
                    log.error("⚠️ 模型返回了 tool_code 格式，说明模型不支持标准 Function Calling");
                }
            }
            
        } catch (Exception e) {
            log.error("调用异常", e);
            fail("工具调用失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试4: 强制工具调用（使用明确指令）")
    public void testForcedToolCall() {
        log.info("========== 测试4: 强制工具调用 ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        ChatClient client = ChatClient.builder(chatModel).build();
        
        // 更明确的提示，要求必须调用工具
        String prompt = "You must call the text_to_sql tool with this SQL: SELECT * FROM users WHERE id = 1";
        
        log.info("发送 Prompt: {}", prompt);
        
        try {
            ChatResponse response = client.prompt(prompt)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .chatResponse();
            
            var generation = response.getResults().get(0);
            boolean hasToolCalls = generation.getOutput().getToolCalls() != null 
                && !generation.getOutput().getToolCalls().isEmpty();
            
            log.info("FinishReason: {}", generation.getMetadata().getFinishReason());
            log.info("是否触发工具调用: {}", hasToolCalls);
            log.info("响应内容: {}", generation.getOutput().getText());
            
            if (!hasToolCalls) {
                log.error("❌ 即使使用明确指令，工具仍未被调用");
                log.error("这表明模型可能不支持标准的 OpenAI Function Calling");
            }
            
        } catch (Exception e) {
            log.error("调用异常", e);
        }
    }

    @Test
    @DisplayName("测试5: 检查模型配置")
    public void testModelConfiguration() {
        log.info("========== 测试5: 检查模型配置 ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        
        log.info("模型类型: {}", chatModel.getClass().getName());
        log.info("模型默认选项: {}", chatModel.getDefaultOptions());
        
        // 尝试获取更多信息
        try {
            var options = chatModel.getDefaultOptions();
            log.info("模型配置详情: {}", options);
        } catch (Exception e) {
            log.warn("无法获取模型详细配置: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("测试6: 使用 sql_query 工具")
    public void testSqlQueryTool() {
        log.info("========== 测试6: 测试 sql_query 工具调用 ==========");
        
        ChatModel chatModel = modelProviderService.getAndValidateChatModel(TEST_MODEL_ID);
        ChatClient client = ChatClient.builder(chatModel).build();
        
        String prompt = "Execute this query using sql_query tool: " +
                "datasourceId='mysqlDataSource', sql='SELECT COUNT(*) FROM MOCK_DATA'";
        
        log.info("Prompt: {}", prompt);
        
        try {
            ChatResponse response = client.prompt(prompt)
                    .toolCallbacks(toolCallbacks)
                    .call()
                    .chatResponse();
            
            var generation = response.getResults().get(0);
            boolean hasToolCalls = generation.getOutput().getToolCalls() != null 
                && !generation.getOutput().getToolCalls().isEmpty();
            
            log.info("工具调用结果: {}", hasToolCalls);
            log.info("响应: {}", generation.getOutput().getText());
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }
}


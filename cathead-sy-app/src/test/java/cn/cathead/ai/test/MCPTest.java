package cn.cathead.ai.test;

import io.modelcontextprotocol.client.McpClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MCPTest {


    @Resource
    private ChatClient chatClient;



    @Test
    public void test_tool() {
        String userInput = "有哪些工具可以使用";
        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().chatResponse());
    }

    @Test
    public void test_workflow() {
        String userInput = "获取电脑配置";
        userInput = "在 C:/Users/15505/Desktop 文件夹下，创建 电脑.txt";

        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().chatResponse());
    }

}
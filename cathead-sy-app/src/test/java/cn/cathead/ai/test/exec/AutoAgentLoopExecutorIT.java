package cn.cathead.ai.test.exec;

import cn.cathead.ai.Application;
import cn.cathead.ai.domain.exec.model.entity.ExecuteCommandEntity;
import cn.cathead.ai.domain.exec.service.chain.AutoAgentLoopExecutor;
import cn.cathead.ai.domain.model.service.ModelService;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import java.io.IOException;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("dev")
@DisplayName("AutoAgentLoopExecutor 关键链路集成测试")
class AutoAgentLoopExecutorIT {

    @Autowired
    private AutoAgentLoopExecutor autoAgentLoopExecutor;

    @Autowired
    private ModelService modelService;
    private static class TestEmitter extends ResponseBodyEmitter {
        private final List<String> events = new ArrayList<>();
        private final CountDownLatch done = new CountDownLatch(1);

        @Override
        public void send(Object data) throws IOException {
            synchronized (events) {
                events.add(data == null ? null : String.valueOf(data));
            }
            System.out.println("[SSE] " + (data == null ? "null" : String.valueOf(data)));
            System.out.flush();
        }

        @Override
        public void complete() {
            done.countDown();
            System.out.println("[SSE] <complete>");
            System.out.flush();
        }

        public boolean awaitDone(Duration timeout) throws InterruptedException {
            return done.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public List<String> getEvents() {
            synchronized (events) {
                return new ArrayList<>(events);
            }
        }
    }


    @Test
    @DisplayName("执行聊天测试")
    void testChat() throws Exception {
        String modelId = "7c5d376d-3bf6-41dd-a5dc-a7390ae09a18";
        ChatRequestDTO chatRequestDTO=new ChatRequestDTO();
        chatRequestDTO.setModelId(modelId);
        chatRequestDTO.setPrompt("能不能测试通过啊到底");
        chatRequestDTO.setStream(false);
        System.out.println(modelService.chatWith(chatRequestDTO));



    }
    @Test
    @DisplayName("执行一轮链路并断言结束事件")
    void testExecuteOnce() throws Exception {
        String modelId = "741f040a-330c-42c4-a7bc-dbdd36010f8a";

        ExecuteCommandEntity cmd = ExecuteCommandEntity.builder()
                .modelId(modelId)
                .sessionId("test-session")
                .agentId("test-agent")
                .knowledgeId("test-knowledge")
                .maxStep(1)
                .task("在 mysqlDataSource 的 MOCK_DATA 表中统计性别为 Male 和 Female 的人数，并且输出各自的人数")
                .build();

        TestEmitter emitter = new TestEmitter();

        autoAgentLoopExecutor.execute(cmd, emitter);

        boolean finished = emitter.awaitDone(Duration.ofSeconds(30));
        Assertions.assertTrue(finished, "执行未在超时内完成");

        List<String> events = emitter.getEvents();
        Assertions.assertFalse(events.isEmpty(), "未捕获到任何事件");

        boolean hasFinish = events.stream().anyMatch(s -> s.contains("\"stage\":\"System\"") && s.contains("\"content\":\"FINISH\""));
        Assertions.assertTrue(hasFinish, "未收到结束事件FINISH: " + events);
    }

//    @Test
//    @DisplayName("执行一轮链路并断言结束事件")
//    void testTool() throws Exception {
//        String modelId = "741f040a-330c-42c4-a7bc-dbdd36010f8a";
//        ChatModel chatModel=modelService.getLatestChatModel(modelId);
//        ChatClient chatClient=ChatClient.builder()
//                .defaultTools()
//                .build();
//
//
//    }
//
//    @Tool
//    public void printsss(){
//        System.out.println("sss");
//    }


}

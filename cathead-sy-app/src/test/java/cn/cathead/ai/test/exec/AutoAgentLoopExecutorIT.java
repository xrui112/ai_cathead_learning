package cn.cathead.ai.test.exec;

import cn.cathead.ai.Application;
import cn.cathead.ai.domain.exec.model.entity.ExecuteCommandEntity;
import cn.cathead.ai.domain.exec.model.entity.Emitter;
import cn.cathead.ai.domain.exec.service.facade.AutoAgentLoopExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("dev")
@DisplayName("AutoAgentLoopExecutor 关键链路集成测试")
class AutoAgentLoopExecutorIT {

    @Autowired
    private AutoAgentLoopExecutor autoAgentLoopExecutor;

    private static class TestEmitter implements Emitter<String> {
        private final List<String> events = new ArrayList<>();
        private final CountDownLatch done = new CountDownLatch(1);

        @Override
        public void send(String data) {
            synchronized (events) {
                events.add(data);
            }
        }

        @Override
        public void complete() {
            done.countDown();
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
    @DisplayName("执行一轮链路并断言结束事件")
    void testExecuteOnce() throws Exception {
        String modelId = "7c5d376d-3bf6-41dd-a5dc-a7390ae09a18";

        ExecuteCommandEntity cmd = ExecuteCommandEntity.builder()
                .modelId(modelId)
                .sessionId("test-session")
                .agentId("test-agent")
                .knowledgeId("test-knowledge")
                .maxStep(1)
                .task("say hello")
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
}

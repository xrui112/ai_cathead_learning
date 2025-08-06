package cn.cathead.ai.test.controller;

import cn.cathead.ai.test.data.TestDataBuilder;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev") // 使用开发环境配置
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@DisplayName("ModelManageController 集成测试 - 完整流程")
class ModelManageControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdChatModelId;
    private static String createdEmbeddingModelId;

    @BeforeEach
    void setUp() {
        // 设置WebTestClient超时时间
        webTestClient = webTestClient.mutate()
                .responseTimeout(java.time.Duration.ofSeconds(30))
                .build();
        
        log.info("=== 开始执行测试方法 ===");
    }
    @Nested
    @DisplayName("4. 动态表单测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DynamicFormTests {

        @Test
        @Order(1)
        @DisplayName("获取表单配置 - ollama chat")
        void testGetFormConfigurationOllamaChat() {
            // When & Then
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/manage/model_form/config")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_GET_FORM_CONFIG.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.SUCCESS_GET_FORM_CONFIG.getInfo())
                    .jsonPath("$.data").isNotEmpty()
                    .jsonPath("$.data.provider").isEqualTo("ollama")
                    .jsonPath("$.data.type").isEqualTo("chat")
                    .jsonPath("$.data.fields").isArray()
                    .consumeWith(response -> {
                        log.info("获取表单配置响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @DisplayName("提交表单数据 - 成功流程")
        void testSubmitFormSuccess() {
            // Given
            Map<String, Object> formData = TestDataBuilder.defaultFormData();

            // When：发送请求
            EntityExchangeResult<byte[]> response = webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/manage/model-form/submit")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(formData)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .returnResult();  // 保存响应结果用于后续验证

            // Then：验证响应内容
            byte[] responseBody = response.getResponseBody();
            String responseBodyString = new String(responseBody);

            log.info("提交表单成功响应: {}", responseBodyString);

            // JSON 验证
            JsonPathExpectationsHelper jsonPathHelper = new JsonPathExpectationsHelper("$.code");
            jsonPathHelper.assertValue(responseBodyString, ResponseCode.SUCCESS_SUBMIT_FORM.getCode());
        }


        @Test
        @DisplayName("进行流式响应测试")
        void testStreamChat() {
            // Given
            // ModelID : 6dad303e-56c8-4fdc-9fd8-f13e043daa9d
            ChatRequestDTO chatRequest = new ChatRequestDTO();
            chatRequest.setModelId("6dad303e-56c8-4fdc-9fd8-f13e043daa9d");
            chatRequest.setPrompt("告诉我上海是一座什么样的城市");

            // When & Then：发送请求并验证流式响应
            Flux<String> responseFlux = webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/service/chat-with-stream")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(chatRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType("application/json") // 验证SSE响应头
                    .returnResult(String.class)
                    .getResponseBody();

            // 使用StepVerifier验证流式数据
            StepVerifier.create(responseFlux)
                    .expectSubscription()
                    .thenConsumeWhile(chunk -> {
                        log.info("收到流式响应数据块: {}", chunk);
                        // 验证响应不为空
                        assertThat(chunk).isNotNull();
                        return true;
                    })
                    .expectComplete()
                    .verify(java.time.Duration.ofSeconds(30)); // 30秒超时
        }
        
        @Test
        @DisplayName("流式响应测试 - 简化版本")
        void testStreamChatSimple() {
            // Given
            ChatRequestDTO chatRequest = new ChatRequestDTO();
            chatRequest.setModelId("6dad303e-56c8-4fdc-9fd8-f13e043daa9d");
            chatRequest.setPrompt("简单测试");

            // When：发送请求
            EntityExchangeResult<byte[]> result = webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/service/chat-with-stream")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(chatRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .returnResult();

            // Then：验证响应内容
            byte[] responseBody = result.getResponseBody();
            assertThat(responseBody).isNotNull();
            
            String responseBodyString = new String(responseBody);
            log.info("流式响应内容: {}", responseBodyString);
            
            // 根据实际响应格式进行验证
            assertThat(responseBodyString).isNotEmpty();
        }

        @Test
        @DisplayName("普通聊天测试")
        void testChat() {
            // Given
            ChatRequestDTO chatRequest = new ChatRequestDTO();
            chatRequest.setModelId("6dad303e-56c8-4fdc-9fd8-f13e043daa9d");
            chatRequest.setPrompt("告诉我上海是一座什么样的城市");

            // When：发送请求
            EntityExchangeResult<byte[]> result = webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/service/chat-with")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(chatRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .returnResult();

            // Then：验证响应内容
            byte[] responseBody = result.getResponseBody();
            assertThat(responseBody).isNotNull();

            String responseBodyString = new String(responseBody);
            log.info(responseBodyString);
        }
    }
} 
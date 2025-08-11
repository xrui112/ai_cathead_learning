package cn.cathead.ai.test.controller;

import cn.cathead.ai.Application;
import cn.cathead.ai.test.data.TestDataBuilder;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("dev") // 使用开发环境配置
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
@DisplayName("ModelServiceTest 集成测试 - 完整流程")
class ModelServiceTest {

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
    @DisplayName("动态表单测试")
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
                            .queryParam("type", "embedding")
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
    }
} 
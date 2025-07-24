package cn.cathead.ai.test.controller;

import cn.cathead.ai.test.data.TestDataBuilder;
import cn.cathead.ai.types.dto.ChatModelDTO;
import cn.cathead.ai.types.dto.EmbeddingModelDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;

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
    @DisplayName("1. 模型创建测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ModelCreationTests {

        @Test
        @Order(1)
        @DisplayName("创建Chat模型 - 成功流程")
        void testCreateChatModelSuccess() {
            // Given
            ChatModelDTO chatModelDTO = TestDataBuilder.defaultChatModelDTO().build();
            log.info("测试数据: {}", chatModelDTO);

            // When & Then
            webTestClient.post()
                    .uri("/api/v1/manage/create_chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(chatModelDTO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_CREATE.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.SUCCESS_CREATE.getInfo())
                    .jsonPath("$.data").isNotEmpty()
                    .jsonPath("$.data").value(data -> {
                        assertThat(data).isNotNull();
                        assertThat(data.toString()).isNotBlank();
                        createdChatModelId = data.toString();
                        log.info("创建的Chat模型ID: {}", createdChatModelId);
                    })
                    .consumeWith(response -> {
                        log.info("Chat模型创建响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(2)
        @DisplayName("创建Embedding模型 - 成功流程")
        void testCreateEmbeddingModelSuccess() {
            // Given
            EmbeddingModelDTO embeddingModelDTO = TestDataBuilder.defaultEmbeddingModelDTO().build();
            log.info("测试数据: {}", embeddingModelDTO);

            // When & Then
            webTestClient.post()
                    .uri("/api/v1/manage/create_embedding")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(embeddingModelDTO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_CREATE.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.SUCCESS_CREATE.getInfo())
                    .jsonPath("$.data").isNotEmpty()
                    .jsonPath("$.data").value(data -> {
                        assertThat(data).isNotNull();
                        assertThat(data.toString()).isNotBlank();
                        createdEmbeddingModelId = data.toString();
                        log.info("创建的Embedding模型ID: {}", createdEmbeddingModelId);
                    })
                    .consumeWith(response -> {
                        log.info("Embedding模型创建响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(3)
        @DisplayName("创建Chat模型 - 参数校验失败")
        void testCreateChatModelValidationFailed() {
            // Given - 使用无效数据
            ChatModelDTO invalidChatModelDTO = TestDataBuilder.invalidChatModelDTO().build();
            log.info("无效测试数据: {}", invalidChatModelDTO);

            // When & Then
            webTestClient.post()
                    .uri("/api/v1/manage/create_chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidChatModelDTO)
                    .exchange()
                    .expectStatus().isOk() // Controller层有异常处理，返回200状态
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.FAILED_CREATE.getCode())
                    .jsonPath("$.info").value(info -> {
                        assertThat(info.toString()).contains(ResponseCode.FAILED_CREATE.getInfo());
                    })
                    .jsonPath("$.data").isEmpty()
                    .consumeWith(response -> {
                        log.info("参数校验失败响应: {}", new String(response.getResponseBody()));
                    });
        }
    }

    @Nested
    @DisplayName("2. 模型查询和管理测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ModelManagementTests {

        @Test
        @Order(1)
        @DisplayName("查询不存在的模型")
        void testGetNonExistentModel() {
            // Given
            String nonExistentModelId = TestDataBuilder.nonExistentModelId();

            // When & Then
            webTestClient.get()
                    .uri("/api/v1/manage/model/{modelId}", nonExistentModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.MODEL_NOT_FOUND.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.MODEL_NOT_FOUND.getInfo())
                    .jsonPath("$.data").isEmpty()
                    .consumeWith(response -> {
                        log.info("查询不存在模型响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(2)
        @DisplayName("删除不存在的模型")
        void testDeleteNonExistentModel() {
            // Given
            String nonExistentModelId = TestDataBuilder.nonExistentModelId();

            // When & Then
            webTestClient.delete()
                    .uri("/api/v1/manage/model/{modelId}", nonExistentModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_DELETE.getCode())
                    .consumeWith(response -> {
                        log.info("删除不存在模型响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(3)
        @DisplayName("更新不存在的Chat模型")
        void testUpdateNonExistentChatModel() {
            // Given
            String nonExistentModelId = TestDataBuilder.nonExistentModelId();
            ChatModelDTO updateDTO = TestDataBuilder.updateChatModelDTO().build();

            // When & Then
            webTestClient.put()
                    .uri("/api/v1/manage/chat/{modelId}", nonExistentModelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateDTO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.FAILED_UPDATE_CHAT.getCode())
                    .consumeWith(response -> {
                        log.info("更新不存在Chat模型响应: {}", new String(response.getResponseBody()));
                    });
        }
    }

    @Nested
    @DisplayName("3. 缓存管理测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CacheManagementTests {

        @Test
        @Order(1)
        @DisplayName("获取Bean统计信息")
        void testGetBeanStats() {
            // When & Then
            webTestClient.get()
                    .uri("/api/v1/manage/model/bean/stats")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_GET_BEAN_STATS.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.SUCCESS_GET_BEAN_STATS.getInfo())
                    .jsonPath("$.data").isNotEmpty()
                    .jsonPath("$.data.beanStats").exists()
                    .jsonPath("$.data.chatModelCache").exists()
                    .jsonPath("$.data.embeddingModelCache").exists()
                    .consumeWith(response -> {
                        log.info("Bean统计信息响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(2)
        @DisplayName("清空所有模型Bean")
        void testClearAllModelBeans() {
            // When & Then
            webTestClient.post()
                    .uri("/api/v1/manage/model/bean/clear")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_CLEAR_BEANS.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.SUCCESS_CLEAR_BEANS.getInfo())
                    .consumeWith(response -> {
                        log.info("清空Bean响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(3)
        @DisplayName("刷新不存在模型的缓存")
        void testRefreshNonExistentModelCache() {
            // Given
            String nonExistentModelId = TestDataBuilder.nonExistentModelId();

            // When & Then
            webTestClient.post()
                    .uri("/api/v1/manage/model/{modelId}/refresh", nonExistentModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.FAILED_REFRESH_CACHE.getCode())
                    .consumeWith(response -> {
                        log.info("刷新不存在模型缓存响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(4)
        @DisplayName("批量刷新所有模型缓存")
        void testRefreshAllModelCache() {
            // When & Then
            webTestClient.post()
                    .uri("/api/v1/manage/model/refresh/all")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_REFRESH_ALL_CACHE.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.SUCCESS_REFRESH_ALL_CACHE.getInfo())
                    .consumeWith(response -> {
                        log.info("批量刷新缓存响应: {}", new String(response.getResponseBody()));
                    });
        }
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
        @Order(2)
        @DisplayName("获取表单配置 - 不支持的提供商")
        void testGetFormConfigurationUnsupportedProvider() {
            // When & Then
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/manage/model_form/config")
                            .queryParam("provider", "unsupported-provider")
                            .queryParam("type", "chat")
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.UNSUPPORTED_PROVIDER_TYPE.getCode())
                    .consumeWith(response -> {
                        log.info("不支持的提供商响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(3)
        @DisplayName("校验表单数据 - 有效数据")
        void testValidateFormDataValid() {
            // Given
            Map<String, Object> formData = TestDataBuilder.defaultFormData();

            // When & Then
            webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/manage/model_form/validate")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(formData)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_VALIDATE_FORM.getCode())
                    .jsonPath("$.info").isEqualTo(ResponseCode.SUCCESS_VALIDATE_FORM.getInfo())
                    .jsonPath("$.data.valid").isEqualTo(true)
                    .consumeWith(response -> {
                        log.info("表单数据校验成功响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(4)
        @DisplayName("校验表单数据 - 无效数据")
        void testValidateFormDataInvalid() {
            // Given
            Map<String, Object> invalidFormData = TestDataBuilder.invalidFormData();

            // When & Then
            webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/manage/model_form/validate")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidFormData)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.FAILED_VALIDATE_FORM.getCode())
                    .jsonPath("$.data.valid").isEqualTo(false)
                    .jsonPath("$.data.errors").isMap()
                    .consumeWith(response -> {
                        log.info("表单数据校验失败响应: {}", new String(response.getResponseBody()));
                    });
        }

        @Test
        @Order(5)
        @DisplayName("提交表单数据 - 成功流程")
        void testSubmitFormSuccess() {
            // Given
            Map<String, Object> formData = TestDataBuilder.defaultFormData();

            // When & Then
            webTestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/manage/model_form/submit")
                            .queryParam("provider", "ollama")
                            .queryParam("type", "chat")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(formData)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_SUBMIT_FORM.getCode())
                    .consumeWith(response -> {
                        log.info("提交表单成功响应: {}", new String(response.getResponseBody()));
                    });
        }
    }

    @Nested
    @DisplayName("5. 完整CRUD流程测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CompleteCRUDTests {

        private String testChatModelId;
        private String testEmbeddingModelId;

        @Test
        @Order(1)
        @DisplayName("完整Chat模型CRUD流程")
        void testCompleteChatModelCRUD() {
            // Step 1: 创建Chat模型
            ChatModelDTO createDTO = TestDataBuilder.defaultChatModelDTO().build();
            
            // 创建模型并获取modelId
            webTestClient.post()
                    .uri("/api/v1/manage/create_chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createDTO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_CREATE.getCode())
                    .jsonPath("$.data").isNotEmpty()
                    .jsonPath("$.data").value(data -> {
                        testChatModelId = data.toString();
                        log.info("创建的Chat模型ID: {}", testChatModelId);
                    });

            // Step 2: 更新Chat模型配置（模拟场景）
            ChatModelDTO updateDTO = TestDataBuilder.updateChatModelDTO().build();
            
            webTestClient.put()
                    .uri("/api/v1/manage/chat/{modelId}", testChatModelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateDTO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("更新Chat模型响应: {}", new String(response.getResponseBody()));
                    });

            // Step 3: 查询模型信息
            webTestClient.get()
                    .uri("/api/v1/manage/model/{modelId}", testChatModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("查询Chat模型响应: {}", new String(response.getResponseBody()));
                    });

            // Step 4: 刷新模型缓存
            webTestClient.post()
                    .uri("/api/v1/manage/model/{modelId}/refresh", testChatModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("刷新Chat模型缓存响应: {}", new String(response.getResponseBody()));
                    });

            // Step 5: 删除模型
            webTestClient.delete()
                    .uri("/api/v1/manage/model/{modelId}", testChatModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("删除Chat模型响应: {}", new String(response.getResponseBody()));
                    });

            log.info("=== Chat模型CRUD流程测试完成 ===");
        }

//        @AfterEach
//        void cleanupCRUDTestData() {
//            // 清理CRUD测试中创建的数据
//            if (testChatModelId != null) {
//                try {
//                    webTestClient.delete()
//                            .uri("/api/v1/manage/model/{modelId}", testChatModelId)
//                            .exchange();
//                    log.info("清理CRUD测试数据 - Chat模型ID: {}", testChatModelId);
//                } catch (Exception e) {
//                    log.warn("清理CRUD测试Chat模型失败: {}", e.getMessage());
//                }
//                testChatModelId = null;
//            }
//
//            if (testEmbeddingModelId != null) {
//                try {
//                    webTestClient.delete()
//                            .uri("/api/v1/manage/model/{modelId}", testEmbeddingModelId)
//                            .exchange();
//                    log.info("清理CRUD测试数据 - Embedding模型ID: {}", testEmbeddingModelId);
//                } catch (Exception e) {
//                    log.warn("清理CRUD测试Embedding模型失败: {}", e.getMessage());
//                }
//                testEmbeddingModelId = null;
//            }
//        }

        @Test
        @Order(2)
        @DisplayName("完整Embedding模型CRUD流程")
        void testCompleteEmbeddingModelCRUD() {
            // Step 1: 创建Embedding模型
            EmbeddingModelDTO createDTO = TestDataBuilder.defaultEmbeddingModelDTO().build();
            
            // 创建模型并获取modelId
            webTestClient.post()
                    .uri("/api/v1/manage/create_embedding")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createDTO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.SUCCESS_CREATE.getCode())
                    .jsonPath("$.data").isNotEmpty()
                    .jsonPath("$.data").value(data -> {
                        testEmbeddingModelId = data.toString();
                        log.info("创建的Embedding模型ID: {}", testEmbeddingModelId);
                    });

            // Step 2: 更新Embedding模型配置
            EmbeddingModelDTO updateDTO = TestDataBuilder.updateEmbeddingModelDTO().build();
            
            webTestClient.put()
                    .uri("/api/v1/manage/embedding/{modelId}", testEmbeddingModelId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateDTO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("更新Embedding模型响应: {}", new String(response.getResponseBody()));
                    });

            // Step 3: 查询模型信息
            webTestClient.get()
                    .uri("/api/v1/manage/model/{modelId}", testEmbeddingModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("查询Embedding模型响应: {}", new String(response.getResponseBody()));
                    });

            // Step 4: 删除模型
            webTestClient.delete()
                    .uri("/api/v1/manage/model/{modelId}", testEmbeddingModelId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("删除Embedding模型响应: {}", new String(response.getResponseBody()));
                    });

            log.info("=== Embedding模型CRUD流程测试完成 ===");
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    @DisplayName("6. 异常处理和边界测试")
    class ExceptionAndBoundaryTests {

        @Test
        @Order(1)
        @DisplayName("测试空请求体")
        void testEmptyRequestBody() {
            webTestClient.post()
                    .uri("/api/v1/manage/create_chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isOk() // Controller有异常处理，返回200状态
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResponseCode.FAILED_CREATE.getCode())
                    .jsonPath("$.info").value(info -> {
                        assertThat(info.toString()).contains(ResponseCode.FAILED_CREATE.getInfo());
                    })
                    .consumeWith(response -> {
                        log.info("空请求体响应: {}", new String(response.getResponseBody()));
                    });
        }



        @Test
        @Order(2)
        @DisplayName("测试缺少必需参数的动态表单查询")
        void testMissingRequiredParams() {
            webTestClient.get()
                    .uri("/api/v1/manage/model_form/config")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .consumeWith(response -> {
                        log.info("缺少参数响应: {}", new String(response.getResponseBody()));
                    });
        }
    }
} 
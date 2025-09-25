package cn.cathead.ai.test.controller;

import cn.cathead.ai.Application;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("dev")
@Slf4j
@DisplayName("简化模型服务测试")
class SimpleModelServiceTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        // 设置WebTestClient超时时间
        webTestClient = webTestClient.mutate()
                .responseTimeout(java.time.Duration.ofSeconds(30))
                .build();

        log.info("=== 开始执行测试方法 ===");
    }

    @Test
    @DisplayName("测试")
    void testOpenai() {
        // Given
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("http://localhost:11434")
                .apiKey("")
                .build();
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen3:32b")
                        .build())
                .build();

        System.out.println(openAiChatModel.call("侬好伐"));
    }

    @Test
    @DisplayName("测试流式纯文本聊天接口")
    void testChatStream() {
        ChatRequestDTO chatRequest = new ChatRequestDTO();
        chatRequest.setModelId("7c5d376d-3bf6-41dd-a5dc-a7390ae09a18");
        chatRequest.setPrompt("告诉我上海是一座什么样的城市");
        chatRequest.setStream(true);
        chatRequest.setOnlyText(true);

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/chat-with")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(chatRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(chunk -> log.info("SSE: {}", chunk))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("测试流式非纯文本聊天接口")
    void testChatStreamText() {
        ChatRequestDTO chatRequest = new ChatRequestDTO();
        chatRequest.setModelId("6dad303e-56c8-4fdc-9fd8-f13e043daa9d");
        chatRequest.setPrompt("告诉我上海是一座什么样的城市");
        chatRequest.setStream(true);
        chatRequest.setOnlyText(false);

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/chat-with")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(chatRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(chunk -> log.info("SSE: {}", chunk))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("测试非流式非纯文本聊天接口")
    void testChat() {
        ChatRequestDTO chatRequest = new ChatRequestDTO();
        chatRequest.setModelId("6dad303e-56c8-4fdc-9fd8-f13e043daa9d");
        chatRequest.setPrompt("告诉我上海是一座什么样的城市");
        chatRequest.setStream(false);
        chatRequest.setOnlyText(false);

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/chat-with")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(res -> log.info(" {}", res))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("测试非流式纯文本聊天接口")
    void testChatText() {
        ChatRequestDTO chatRequest = new ChatRequestDTO();
        chatRequest.setModelId("6dad303e-56c8-4fdc-9fd8-f13e043daa9d");
        chatRequest.setPrompt("告诉我上海是一座什么样的城市");
        chatRequest.setStream(false);
        chatRequest.setOnlyText(true);

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/chat-with")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(res -> log.info("{}", res))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("测试非流式 image 文本聊天接口")
    void testImageText() throws IOException {
        ChatRequestDTO chatRequest = new ChatRequestDTO();
        chatRequest.setModelId("eca48c99-2871-434d-9995-b570cfc56111");
        chatRequest.setPrompt("这张图片关于什么");
        chatRequest.setStream(false);
        chatRequest.setOnlyText(true);


        // 加载并转为 byte[]
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("images/dog.jpg");
        assert inputStream != null;
        byte[] imageBytes = inputStream.readAllBytes();


        chatRequest.setImage(imageBytes);

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/chat-with")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(res -> log.info("{}", res))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("测试非流式 image 聊天接口")
    void testImage() throws IOException {
        ChatRequestDTO chatRequest = new ChatRequestDTO();
        chatRequest.setModelId("eca48c99-2871-434d-9995-b570cfc56111");
        chatRequest.setPrompt("这张图片关于什么");
        chatRequest.setStream(false);
        chatRequest.setOnlyText(false);

        // 加载并转为 byte[]
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("images/dog.jpg");
        assert inputStream != null;
        byte[] imageBytes = inputStream.readAllBytes();
        chatRequest.setImage(imageBytes);

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/chat-with")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(res -> log.info("{}", res))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("测试文本向量化接口 单文本")
    void testEmbedTextSingle() {


        EmbeddingRequestDTO request = new EmbeddingRequestDTO("a0ab2e45-948e-46ec-95b2-7c7da0281daf",
                "测试测试测试测试");

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/embed-text")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(res -> log.info("响应结果：{}", res))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("测试文本向量化接口 多文本")
    void testEmbedTextBatch() {
        List<String> texts = List.of("测试测试测试测试1", "测试测试测试测试2", "测试测试测试测试3");
        EmbeddingRequestDTO request = new EmbeddingRequestDTO("1dd5dc71-2bc3-43ad-9dcb-bfe88582a46f", texts);

        webTestClient.mutate().responseTimeout(java.time.Duration.ofSeconds(60)).build()
                .post()
                .uri("/api/v1/service/embed-text")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseBody()
                .doOnNext(res -> log.info("响应结果：{}", res))
                .blockLast(java.time.Duration.ofSeconds(60));
    }

} 
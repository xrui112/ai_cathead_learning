package cn.cathead.ai.trigger.http;

import cn.cathead.ai.domain.model.service.IModelService;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.dto.EmbeddingRequestDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import cn.cathead.ai.types.model.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 模型相关的服务接口
 * URL
 * base  /api/v1/service
 * /chat_with 调用chat模型
 * /embedding_with 调用embedding模型
 */
@RestController
@RequestMapping("/api/v1/service")
@Slf4j
public class ModelServiceController {

    @Resource
    private IModelService modelService;

    @PostMapping("chat-with")
    public ResponseEntity<?> chatWith(@RequestBody ChatRequestDTO chatRequestDto) {
        try {
            boolean stream = Boolean.TRUE.equals(chatRequestDto.getStream());
            boolean onlyText = Boolean.TRUE.equals(chatRequestDto.getOnlyText());
            if (stream) {
                if (!onlyText) {
                    log.info("流式响应 非纯文本启动");
                    Flux<ServerSentEvent<ChatResponse>> sse = modelService.chatWithStream(chatRequestDto)
                            .map(resp -> ServerSentEvent.<ChatResponse>builder()
                                    .id(String.valueOf(System.currentTimeMillis()))
                                    .event("message")
                                    .data(resp)
                                    .build());


                    return ResponseEntity.ok()

                            .contentType(MediaType.TEXT_EVENT_STREAM)
                            .body(sse);
                }
                log.info("流式响应 纯文本启动");
                Flux<ServerSentEvent<String>> sseText = modelService.chatWithStream(chatRequestDto)
                        .map(resp -> ServerSentEvent.<String>builder()
                                .id(String.valueOf(System.currentTimeMillis()))
                                .event("message")
                                .data(resp.getResults().get(0).getOutput().getText())
                                .build());

                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                         .body(sseText);

            } else {
                if (!onlyText) {
                    log.info("普通响应 非纯文本启动");
                    ChatResponse response = modelService.chatWith(chatRequestDto);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new Response<>(ResponseCode.SUCCESS_CHAT.getCode(),
                                    ResponseCode.SUCCESS_CHAT.getInfo(),
                                    response));
                }
                log.info("普通响应 纯文本启动");
                String response = modelService.chatWith(chatRequestDto).getResults().get(0).getOutput().getText();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new Response<>(ResponseCode.SUCCESS_CHAT.getCode(),
                                ResponseCode.SUCCESS_CHAT.getInfo(),
                                response));
            }
        } catch (AppException e) {
            // 专门处理业务异常
            log.info("聊天请求失败, 错误信息: {}", e.getInfo());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Response<>(e.getCode(), e.getInfo(), null));
        } catch (Exception e) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new Response<>(ResponseCode.FAILED.getCode(),
                            ResponseCode.FAILED.getInfo(), null));
        }
    }

    /**
     * 文本向量化接口
     */
    @RequestMapping(value = "embed-text", method = RequestMethod.POST)
    public Response<EmbeddingResponse> embedText(@RequestBody EmbeddingRequestDTO embeddingRequestDto) {
        try {
            log.info("模型 {} 调用文本向量化请求", embeddingRequestDto.getModelId());
            EmbeddingResponse embeddingResponse = modelService.embedText(embeddingRequestDto);
            return new Response<>(ResponseCode.SUCCESS_EMBEDDING.getCode(), "文本向量化成功", embeddingResponse);
        } catch (AppException e) {
            log.info("模型{} 文本向量化失败,{}", embeddingRequestDto.getModelId(), e.getInfo());
            return new Response<>(e.getCode(), e.getInfo(), null);
        } catch (Exception e) {
            log.warn("模型 {} 文本向量化失败,未知错误: {}", embeddingRequestDto.getModelId(), e.getMessage());
            return new Response<>(ResponseCode.FAILED.getCode(), ResponseCode.FAILED.getInfo(), null);
        }
    }

}
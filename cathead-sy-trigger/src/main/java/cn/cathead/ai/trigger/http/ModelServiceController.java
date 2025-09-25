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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                log.info("流式响应 启动, onlyText={}", onlyText);
                ResponseBodyEmitter emitter = new ResponseBodyEmitter(Long.MAX_VALUE);
                ObjectMapper objectMapper = new ObjectMapper();

                modelService.chatWithStream(chatRequestDto)
                        .subscribe(resp -> {
                                    try {
                                        String idLine = "id: " + System.currentTimeMillis() + "\n";
                                        String eventLine = "event: message\n";
                                        String dataPayload;
                                        if (onlyText) {
                                            String text = resp.getResults().get(0).getOutput().getText();
                                            dataPayload = text == null ? "" : text;
                                        } else {
                                            dataPayload = objectMapper.writeValueAsString(resp);
                                        }
                                        String frame = idLine + eventLine + "data: " + dataPayload + "\n\n";
                                        emitter.send(frame);
                                    } catch (Exception ex) {
                                        try { emitter.completeWithError(ex); } catch (Exception ignore) {}
                                    }
                                },
                                e -> {
                                    try { emitter.completeWithError(e); } catch (Exception ignore) {}
                                },
                                () -> {
                                    try { emitter.complete(); } catch (Exception ignore) {}
                                });

                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(emitter);

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
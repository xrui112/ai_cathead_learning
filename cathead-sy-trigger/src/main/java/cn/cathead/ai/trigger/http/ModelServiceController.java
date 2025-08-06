package cn.cathead.ai.trigger.http;

import cn.cathead.ai.domain.model.service.IModelService;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.AppException;
import cn.cathead.ai.types.model.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
/**
 * 模型相关的服务接口
 * URL
 * base  /api/v1/service
 * /chat_with 调用chat模型
 * /embedding_with 调用embedding模型
 *
 */
@RestController
@RequestMapping("/api/v1/service")
@Slf4j
public class ModelServiceController {

    @Resource
    private IModelService modelService;

    @RequestMapping(value = "chat-with-stream",method = RequestMethod.POST)
    public Flux<org.springframework.ai.chat.model.ChatResponse> chatWithStream(@RequestBody ChatRequestDTO chatRequestDto) {
        return modelService.chatWithStream(chatRequestDto);
    }


    @RequestMapping(value = "chat-with", method = RequestMethod.POST)
    public Response<ChatResponse> chatWith(@RequestBody ChatRequestDTO chatRequestDto) {
        try {
            log.info("模型 {} 调用普通chat请求 {}", chatRequestDto.getModelId(), chatRequestDto.getPrompt());
            ChatResponse chatResponse = modelService.chatWith(chatRequestDto);
            return new Response<>(ResponseCode.SUCCESS_CHAT.getCode(), ResponseCode.SUCCESS_CHAT.getInfo(), chatResponse);
        } catch (AppException e) {
            log.info("模型{} 不存在,{}", chatRequestDto.getModelId(), e.getInfo());
            return new Response<>(e.getCode(), e.getInfo(), null);
        }catch (Exception e){
            log.warn("模型 {}调用chat失败,未知错误", chatRequestDto.getModelId());
            return new Response<>(ResponseCode.FAILED.getCode(), ResponseCode.FAILED.getInfo(), null);
        }
    }








}
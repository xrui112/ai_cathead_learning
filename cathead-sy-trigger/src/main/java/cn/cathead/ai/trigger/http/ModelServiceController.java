package cn.cathead.ai.trigger.http;

import cn.cathead.ai.domain.model.service.IModelService;
import cn.cathead.ai.types.dto.ChatRequestDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

    @RequestMapping(value = "chat_with",method = RequestMethod.POST)
    public Flux<org.springframework.ai.chat.model.ChatResponse> chatWith(@RequestBody ChatRequestDTO chatRequestDto) {
        return modelService.chatWith(chatRequestDto);
    }

}
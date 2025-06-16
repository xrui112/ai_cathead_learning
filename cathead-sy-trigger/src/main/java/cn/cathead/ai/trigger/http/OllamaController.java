package cn.cathead.ai.trigger.http;

import cn.cathead.ai.api.IAiService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama/")
public class OllamaController implements IAiService {

    @Resource
    private OllamaChatModel  ollamaChatModel;



    //  curl "http://localhost:8090/api/v1/ollama/generate?model=deepseek-r1:1.5b&message=1+1="
    @RequestMapping(value = "generate",method = RequestMethod.GET)
    @Override
    public ChatResponse genarate(@RequestParam String model,@RequestParam String message) {

        return ollamaChatModel.call(new Prompt(
                message,
                ChatOptions.builder().model(model).build()

        ));
    }

    // curl "http://localhost:8090/api/v1/ollama/generate_stream?model=deepseek-r1:1.5b&message=1+1="
    @RequestMapping(value = "generate_stream",method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> genarateStream(@RequestParam String model, @RequestParam String message) {

        return ollamaChatModel.stream(new Prompt(
                message,
                ChatOptions.builder()
                        .model(model)
                        .build()
        ));
    }




}

package cn.cathead.ai.api;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

public interface IAiService {

    public ChatResponse generate(String model, String message);

    public Flux<ChatResponse> generateStream( String model,  String message);
    public Flux<ChatResponse> generateStreamRag(String modle, String ragTag,String message);

}

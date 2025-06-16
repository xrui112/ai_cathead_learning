package cn.cathead.ai.api;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

public interface IAiService {

    public ChatResponse genarate(String model, String message);

    public Flux<ChatResponse> genarateStream( String model,  String message);
}

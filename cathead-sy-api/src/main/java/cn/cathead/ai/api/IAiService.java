package cn.cathead.ai.api;

import cn.cathead.ai.api.dto.BaseModelDTO;
import cn.cathead.ai.api.dto.ChatModelDTO;
import cn.cathead.ai.api.dto.ChatRequestDto;
import cn.cathead.ai.api.dto.EmbeddingModelDTO;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

public interface IAiService {

    @RequestMapping(value = "creat/model/chat",method = RequestMethod.POST)
    void creatChat(@RequestParam ChatModelDTO chatModelDTO);

    @RequestMapping(value = "creat/model/embedding",method = RequestMethod.POST)
    void creatEmbedding(@RequestParam EmbeddingModelDTO embeddingModelDTO);

    public Flux<ChatResponse> chatWith(ChatRequestDto chatRequestDto);


//    public ChatResponse generate(String model, String message);
//
//    public Flux<ChatResponse> generateStream( String model,  String message);
//    public Flux<ChatResponse> generateStreamRag(String modle, String ragTag,String message);

}

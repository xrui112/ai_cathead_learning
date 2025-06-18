package cn.cathead.ai.trigger.http;

import cn.cathead.ai.api.IAiService;
import jakarta.annotation.Resource;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama/")
public class OllamaController implements IAiService {

    @Resource
    private OllamaChatModel  ollamaChatModel;

    @Resource
    private PgVectorStore pgVectorStore;



    //  curl "http://localhost:8090/api/v1/ollama/generate?model=deepseek-r1:1.5b&message=1+1="
    @RequestMapping(value = "generate",method = RequestMethod.GET)
    @Override
    public ChatResponse generate(@RequestParam String model,@RequestParam String message) {

        return ollamaChatModel.call(new Prompt(
                message,
                ChatOptions.builder().model(model).build()

        ));
    }

    // curl "http://localhost:8090/api/v1/ollama/generate_stream?model=deepseek-r1:1.5b&message=1+1="
    @RequestMapping(value = "generate_stream",method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStream(@RequestParam String model, @RequestParam String message) {

        return ollamaChatModel.stream(new Prompt(
                message,
                ChatOptions.builder()
                        .model(model)
                        .build()
        ));
    }




    @RequestMapping(value = "generate_stream_rag",method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStreamRag(@RequestParam String model,@RequestParam String ragTag,@RequestParam String message){
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;
        SearchRequest request=SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("knowledge == "+ragTag)
                .build();

        List<Document> documents=pgVectorStore.similaritySearch(request);

        String docMerge=documents.stream().map(Document::getText).collect(Collectors.joining());

        Message ms=new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", docMerge));

        List<Message> messages=new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ms);
        return ollamaChatModel.stream(new Prompt(
                messages,
                OllamaOptions.builder()
                        .model(model)
                        .build()
        ));
    }
}

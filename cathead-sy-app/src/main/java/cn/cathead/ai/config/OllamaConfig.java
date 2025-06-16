package cn.cathead.ai.config;


import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl){
        return new OllamaApi.Builder().baseUrl(baseUrl).build();
    }

    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        return new OllamaChatModel(ollamaApi, OllamaOptions.builder().build(),
                ToolCallingManager.builder().build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.builder().build());
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

//    @Bean
//    public SimpleVectorStore vectorStore(@Value("${spring.ai.rag.embed}") String model, OllamaApi ollamaApi, OpenAiApi openAiApi) {
//        if ("nomic-embed-text".equalsIgnoreCase(model)) {
//            OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
//            embeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
//            return new SimpleVectorStore(embeddingClient);
//        } else {
//            OpenAiEmbeddingClient embeddingClient = new OpenAiEmbeddingClient(openAiApi);
//            return new SimpleVectorStore(embeddingClient);
//        }
//    }
//
//    @Bean
//    public PgVectorStore pgVectorStore(@Value("${spring.ai.rag.embed}") String model, OllamaApi ollamaApi, OpenAiApi openAiApi, JdbcTemplate jdbcTemplate) {
//        if ("nomic-embed-text".equalsIgnoreCase(model)) {
//            OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
//            embeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
//            return new PgVectorStore(jdbcTemplate, embeddingClient);
//        } else {
//            OpenAiEmbeddingClient embeddingClient = new OpenAiEmbeddingClient(openAiApi);
//            return new PgVectorStore(jdbcTemplate, embeddingClient);
//        }
//    }
}

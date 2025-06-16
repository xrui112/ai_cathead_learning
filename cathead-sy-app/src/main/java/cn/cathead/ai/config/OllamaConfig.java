package cn.cathead.ai.config;


import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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

    /**
     * @param ollamaApi
     * @return 文档信息缓存到内存
     */
    @Bean
    public SimpleVectorStore vectorStore(OllamaApi ollamaApi) throws Exception {

            OllamaEmbeddingModel embeddingModel = new OllamaEmbeddingModel(ollamaApi,
                    OllamaOptions.builder().model("nomic-embed-text").build(),
                    ObservationRegistry.NOOP,
                    ModelManagementOptions.builder().build());

            return SimpleVectorStore.builder(embeddingModel).build();

    }

    /**
     *
     * @param ollamaApi
     * @return 文档信息缓存到内存
     */
    @Bean
    public PgVectorStore pgVectorStore( OllamaApi ollamaApi, JdbcTemplate jdbcTemplate) throws Exception {

            OllamaEmbeddingModel embeddingModel = new OllamaEmbeddingModel(ollamaApi,
                    OllamaOptions.builder().model("nomic-embed-text").build(),
                    ObservationRegistry.NOOP,
                    ModelManagementOptions.builder().build());

            return PgVectorStore.builder(jdbcTemplate,embeddingModel).build();

    }
}

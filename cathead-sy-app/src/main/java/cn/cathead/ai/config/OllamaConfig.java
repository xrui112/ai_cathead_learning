package cn.cathead.ai.config;


import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.ollama.OllamaEmbeddingModel;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Configuration
public class OllamaConfig {
    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl){
        return new OllamaApi.Builder()
                .baseUrl(baseUrl)
                .build();
    }

    //当我想引入其他模型的时候
    //在此处注入不同模型的Api 然后在对应 vectorStore pgVectorStore 根据不同的模型进行构建
    //然后注入不同的ChatModel 在编写不同接口进行区分即可!

    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {

        return new OllamaChatModel(
                ollamaApi,
                OllamaOptions.builder()
                        .model("llama3.2")
                        .build(),
                DefaultToolCallingManager.builder().build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.builder().build());
    }



//    @Bean
//    public ToolCallbackProvider toolCallbackProvider(OllamaChatModel ollamaChatModel) {
//        ToolCallbackProvider toolCallbackProvider=new SynMcp();
//        return ;
//    }


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

            OllamaEmbeddingModel embeddingModel = new OllamaEmbeddingModel(
                    ollamaApi,
                    OllamaOptions.builder().model("nomic-embed-text").build(),
                    ObservationRegistry.NOOP,
                    ModelManagementOptions.builder().build());

            return SimpleVectorStore.builder(embeddingModel)
                    .build();

    }

    /**
     *
     * @param ollamaApi
     * @return 文档信息缓存到内存
     */
    @Bean
    public PgVectorStore pgVectorStore( OllamaApi ollamaApi, JdbcTemplate jdbcTemplate) throws Exception {

            OllamaEmbeddingModel embeddingModel = new OllamaEmbeddingModel(
                    ollamaApi,
                    OllamaOptions.builder().model("nomic-embed-text").build(),
                    ObservationRegistry.NOOP,
                    ModelManagementOptions.builder().build());

            return PgVectorStore.builder(jdbcTemplate,embeddingModel)
                    .vectorTableName("vector_store_ollama_deepseek")
                    .build();

    }

}

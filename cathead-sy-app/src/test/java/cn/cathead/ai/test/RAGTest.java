package cn.cathead.ai.test;


import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import jakarta.annotation.Resource;
import org.junit.Test;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RAGTest {

    @Resource
    private OllamaChatModel chatModel;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
//    @Resource
//    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore;

    @Test
    public void upload() {
        TikaDocumentReader reader = new TikaDocumentReader("./data/test.md");
        List<Document> documents = reader.get();

        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

        documents.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称"));

        documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称"));


//        documentSplitterList.forEach(doc -> {
//            System.out.println("id=" + doc.getId());
//            System.out.println("embedding=" + doc.e());
//            System.out.println("metadata=" + doc.getMetadata());
//        });
        pgVectorStore.accept(documentSplitterList);

        log.info("上传完成");
    }

    @Test
    public void chat(){

        String message="王大瓜，哪年出生";
        String SYSTEM_PROMPT = """
            Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
            If unsure, simply state that you don't know.
            Another thing you need to note is that your reply must be in Chinese!
            DOCUMENTS:
                {documents}
            """;
        // 指定文档搜索
        SearchRequest request = SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("knowledge == '知识库名称'")
                .build();

        List<Document> documents = pgVectorStore.similaritySearch(request);

        String documentCollectors = documents.stream().map(Document::getText).collect(Collectors.joining());

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentCollectors));

        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        ChatResponse chatResponse = chatModel.call(
                new Prompt(
                        messages,
                        OllamaOptions.builder()
                                .model("deepseek-r1:1.5b")
                                .build()
                ));
        log.info("测试结果：{}", JSON.toJSONString(chatResponse));


    }

}
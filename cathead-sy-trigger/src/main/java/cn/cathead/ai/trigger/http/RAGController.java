package cn.cathead.ai.trigger.http;

import cn.cathead.ai.api.IRAGService;
import cn.cathead.ai.api.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/rag/")
public class RAGController implements IRAGService {
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private PgVectorStore pgVectorStore;

    @RequestMapping(value = "/query_rag_tag_list",method = RequestMethod.GET)
    @Override
    public Response<List<String>> queryRagTagList() {

        RList<String> elements=redissonClient.getList("ragTag");
        return Response.<List<String>>builder()
                .code("0000")
                .info("调用成功")
                .data(new ArrayList<>(elements))
                .build();
    }



    /**
     *todo 1  headers =  "content-type=multipart/form-data"是什么
     2  SearchRequest withTopK(5) 是什么
     */
    @RequestMapping(value = "file/upload",method = RequestMethod.POST,headers =  "content-type=multipart/form-data")
    @Override
    public Response<String> uploadFile(@RequestParam String ragTag,@RequestParam("file") List<MultipartFile> files) {
        log.info("上传知识库开始 {}",ragTag);
        for(MultipartFile file:files){
            TikaDocumentReader tkReader=new TikaDocumentReader(file.getResource());
            List<Document> documents=tkReader.get();
            List<Document> documentsSpilit=tokenTextSplitter.apply(documents);

            documents.forEach(doc->doc.getMetadata().put("knowledge",ragTag));

            documentsSpilit.forEach(doc -> doc.getMetadata().put("knowledge",ragTag) );

            pgVectorStore.accept(documentsSpilit);

            RList<String> ragTags= redissonClient.getList("ragTag");
            if(!ragTags.contains(ragTag)){
                ragTags.add(ragTag);
            }
        }

        log.info("上传完成 {}",ragTag);
        return Response.<String>builder()
                .code("0000")
                .info("调用成功")
                .build();
    }


}

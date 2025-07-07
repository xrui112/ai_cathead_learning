//package cn.cathead.ai.trigger.http;
//
//import cn.cathead.ai.api.IRAGService;
//
//import cn.cathead.ai.types.model.Response;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FileUtils;
//import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
//import org.redisson.api.RList;
//import org.redisson.api.RedissonClient;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.reader.tika.TikaDocumentReader;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
//import org.springframework.core.io.PathResource;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.*;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.ArrayList;
//import java.util.List;
//
//
//@Slf4j
//@RestController
//@CrossOrigin("*")
//@RequestMapping("/api/v1/rag/")
//public class RAGController implements IRAGService {
//    @Resource
//    private RedissonClient redissonClient;
//
//    @Resource
//    private TokenTextSplitter tokenTextSplitter;
//    @Resource
//    private PgVectorStore pgVectorStore;
//
//    @RequestMapping(value = "/query_rag_tag_list",method = RequestMethod.GET)
//    @Override
//    public Response<List<String>> queryRagTagList() {
//
//        RList<String> elements=redissonClient.getList("ragTag");
//        return Response.<List<String>>builder()
//                .code("0000")
//                .info("调用成功")
//                .data(new ArrayList<>(elements))
//                .build();
//    }
//
//    /**
//     2  SearchRequest withTopK(5) 是什么
//     */
//    @RequestMapping(value = "file/upload",method = RequestMethod.POST,headers =  "content-type=multipart/form-data")
//    @Override
//    public Response<String> uploadFile(@RequestParam String ragTag,@RequestParam("file") List<MultipartFile> files) {
//        log.info("上传知识库开始 {}",ragTag);
//        for(MultipartFile file:files){
//            TikaDocumentReader tkReader=new TikaDocumentReader(file.getResource());
//            List<Document> documents=tkReader.get();
//            List<Document> documentsSpilit=tokenTextSplitter.apply(documents);
//
//            documents.forEach(doc->doc.getMetadata().put("knowledge",ragTag));
//
//            documentsSpilit.forEach(doc -> doc.getMetadata().put("knowledge",ragTag) );
//
//            pgVectorStore.accept(documentsSpilit);
//
//            RList<String> ragTags= redissonClient.getList("ragTag");
//            if(!ragTags.contains(ragTag)){
//                ragTags.add(ragTag);
//            }
//        }
//
//        log.info("上传完成 {}",ragTag);
//        return Response.<String>builder()
//                .code("0000")
//                .info("调用成功")
//                .build();
//    }
//
//    //开发Git仓库上传RAG功能
//    //没有在前端对接 感觉没啥意思 后面再说吧..
//    @RequestMapping(value = "analyze_git_repository", method = RequestMethod.POST)
//    @Override
//    public Response<String> analyzeGitRepository(@RequestParam String repoUrl, @RequestParam String userName, @RequestParam String token) throws Exception{
//        String localPath = "./git-cloned-repo";
//        String repoProjectName = extractProjectName(repoUrl);
//        log.info("克隆路径：{}", new File(localPath).getAbsolutePath());
//
//        FileUtils.deleteDirectory(new File(localPath));
//
//        Git git = Git.cloneRepository()
//                .setURI(repoUrl)
//                .setDirectory(new File(localPath))
//                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
//                .call();
//
//        // 使用Files.walkFileTree遍历目录
//        Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
//                try {
//                    TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
//                    List<Document> documents = reader.get();
//                    List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
//
//                    documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
//
//                    documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
//
//                    pgVectorStore.accept(documentSplitterList);
//                } catch (Exception e) {
//                    log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
//                }
//
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//                log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
//                return FileVisitResult.CONTINUE;
//            }
//        });
//
//        FileUtils.deleteDirectory(new File(localPath));
//
//        // 添加知识库记录
//        RList<String> elements = redissonClient.getList("ragTag");
//        if (!elements.contains(repoProjectName)) {
//            elements.add(repoProjectName);
//        }
//
//        git.close();
//        log.info("遍历解析路径，上传完成:{}", repoUrl);
//        return Response.<String>builder().code("0000").info("调用成功").build();
//    }
//
//    private String extractProjectName(String repoUrl) {
//        String[] parts = repoUrl.split("/");
//        String projectNameWithGit = parts[parts.length - 1];
//        return projectNameWithGit.replace(".git", "");
//    }
//
//}

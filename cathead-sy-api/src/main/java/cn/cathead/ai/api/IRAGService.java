package cn.cathead.ai.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import cn.cathead.ai.types.model.Response;

import java.util.List;

public interface IRAGService {


    public Response<List<String>> queryRagTagList();


    public Response<String> uploadFile(String ragTag,  List<MultipartFile> files);


    public Response<String> analyzeGitRepository( String repoUrl,  String userName,  String token) throws Exception;
}

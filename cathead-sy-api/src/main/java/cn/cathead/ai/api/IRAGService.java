package cn.cathead.ai.api;


import cn.cathead.ai.api.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IRAGService {


    public Response<List<String>> queryRagTagList();


    public Response<String> uploadFile(String ragTag,  List<MultipartFile> files);

}

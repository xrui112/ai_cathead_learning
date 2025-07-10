package cn.cathead.ai.infrastructure.persistent.dao;
import cn.cathead.ai.infrastructure.persistent.po.ModelConfig;
import cn.cathead.ai.infrastructure.persistent.po.ChatRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IModelDao {

    long  saveModelRecord(ModelConfig modelConfig);

    ModelConfig queryModelById(ChatRequest chatRequest);
    
    // 新增的模型管理方法
    int updateModelRecord(ModelConfig modelConfig);
    
    void deleteModelRecord(String modelId);
    
    ModelConfig queryModelById(String modelId);
}
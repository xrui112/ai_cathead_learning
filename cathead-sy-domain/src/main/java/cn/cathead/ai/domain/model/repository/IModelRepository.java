package cn.cathead.ai.domain.model.repository;

import cn.cathead.ai.domain.model.model.entity.BaseModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatModelEntity;
import cn.cathead.ai.domain.model.model.entity.ChatRequestEntity;


public interface IModelRepository {

    void saveModelRecord(BaseModelEntity baseModelEntity);

    BaseModelEntity queryModelById(ChatRequestEntity chatRequestEntity);

    void updateModelRecord(BaseModelEntity baseModelEntity);
    
    void deleteModelRecord(String modelId);
    
    BaseModelEntity queryModelById(String modelId);

}

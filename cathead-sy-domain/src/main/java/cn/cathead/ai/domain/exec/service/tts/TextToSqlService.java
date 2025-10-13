package cn.cathead.ai.domain.exec.service.tts;

import cn.cathead.ai.domain.exec.model.entity.ChainContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Text-to-SQL 服务：基于意图与 schema/约束生成只读 SQL。
 */
public interface TextToSqlService {

    String generateSql(String intent, JsonNode schemaOrSource, JsonNode constraints, ChainContext chainContext);
}



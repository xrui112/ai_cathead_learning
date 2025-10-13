package cn.cathead.ai.domain.exec.service.processing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * 数据处理流水线：最小支持 group_by/sum/sort。
 */
public interface DataProcessingService {

    List<Map<String, Object>> process(List<Map<String, Object>> rows, JsonNode opsArray);
}



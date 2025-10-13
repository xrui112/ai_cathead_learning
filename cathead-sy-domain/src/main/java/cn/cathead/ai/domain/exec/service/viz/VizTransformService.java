package cn.cathead.ai.domain.exec.service.viz;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * 可视化转换：将表数据转换为前端可直接使用的结构。
 */
public interface VizTransformService {

    Map<String, Object> toEcharts(List<Map<String, Object>> rows, JsonNode vizSpec);
}



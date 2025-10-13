package cn.cathead.ai.domain.exec.service.tools;

import cn.cathead.ai.domain.exec.service.datasource.DataSourceGateway;
import cn.cathead.ai.domain.exec.service.processing.DataProcessingService;
import cn.cathead.ai.domain.exec.service.viz.VizTransformService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsTools {

    private final DataSourceGateway dataSourceGateway;
    private final DataProcessingService dataProcessingService;
    private final VizTransformService vizTransformService;

    @Tool(name = "text_to_sql", description = "验证并优化SQL查询语句")
    public Map<String, Object> textToSql(
            @org.springframework.ai.tool.annotation.ToolParam(description = "SQL查询语句", required = true) String sql) {
        log.info("🔥🔥🔥 [Tool] text_to_sql 被调用了！输入SQL: {}", sql);
        sql = sql == null ? "" : sql.trim();
        if (sql.isEmpty()) {
            return Map.of("error", "missing_sql", "message", "请在参数中提供 sql 字段，且仅限 SELECT 语句");
        }
        String upper = sql.toUpperCase();
        if (!upper.startsWith("SELECT")) {
            return Map.of("error", "only_select_allowed", "message", "只允许 SELECT 语句");
        }
        if (!upper.contains(" LIMIT ")) {
            sql = sql + " LIMIT 1000";
        }
        log.info("🔥🔥🔥 [Tool] text_to_sql 执行完成，输出SQL: {}", sql);
        return Map.of("sql", sql);
    }

    @Tool(name = "sql_query", description = "执行只读SQL查询并返回结果")
    public Map<String, Object> sqlQuery(
            @org.springframework.ai.tool.annotation.ToolParam(description = "数据源ID", required = true) String datasourceId,
            @org.springframework.ai.tool.annotation.ToolParam(description = "SQL查询语句", required = true) String sql) {
        log.info("🔥🔥🔥🔥🔥 [Tool] sql_query 被调用了！！！");
        log.info("🔥🔥🔥 [Tool] datasourceId = {}", datasourceId);
        log.info("🔥🔥🔥 [Tool] sql = {}", sql);
        
        List<Map<String, Object>> rows = dataSourceGateway.query(datasourceId, sql);
        
        log.info("🔥🔥🔥 [Tool] sql_query 执行完成，返回 {} 行数据", rows == null ? 0 : rows.size());
        return Map.of("rows", rows);
    }

    @Tool(name = "data_process", description = "处理数据：分组、聚合、排序等操作")
    public Map<String, Object> dataProcess(
            @org.springframework.ai.tool.annotation.ToolParam(description = "数据行列表", required = true) List<Map<String, Object>> rows,
            @org.springframework.ai.tool.annotation.ToolParam(description = "操作列表", required = true) JsonNode ops) {
        int inSize = rows == null ? 0 : rows.size();
        log.info("[Tool] data_process called, inRows={}, ops={}", inSize, ops);
        List<Map<String, Object>> out = dataProcessingService.process(rows, ops);
        log.info("[Tool] data_process outRows={}", out == null ? 0 : out.size());
        return Map.of("rows", out);
    }

    @Tool(name = "viz_build", description = "将数据转换为ECharts图表配置")
    public Map<String, Object> vizBuild(
            @org.springframework.ai.tool.annotation.ToolParam(description = "数据行列表", required = true) List<Map<String, Object>> rows,
            @org.springframework.ai.tool.annotation.ToolParam(description = "可视化规格", required = true) JsonNode vizSpec) {
        int inSize = rows == null ? 0 : rows.size();
        log.info("[Tool] viz_build called, inRows={}, vizSpec={}", inSize, vizSpec);
        Map<String, Object> option = vizTransformService.toEcharts(rows, vizSpec);
        log.info("[Tool] viz_build optionKeys={}", option == null ? 0 : option.size());
        return Map.of("echartsOption", option);
    }
}



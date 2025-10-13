package cn.cathead.ai.infrastructure.datasource;

import cn.cathead.ai.domain.exec.service.datasource.DataSourceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 简单 SQL 数据源网关实现：通过 datasourceId 映射到 Spring 中的 DataSource Bean 名称。
 * 约定：datasourceId 与 DataSource 的 beanName 相同。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleSqlDataSourceGateway implements DataSourceGateway {

    private final Map<String, DataSource> dataSourceMap;

    @Override
    public List<Map<String, Object>> query(String datasourceId, String sql) {
        DataSource ds = dataSourceMap.get(datasourceId);
        if (ds == null) {
            throw new IllegalArgumentException("未找到数据源: " + datasourceId);
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        String sqlPreview = sql == null ? "" : sql.substring(0, Math.min(200, sql.length()));
        log.info("[DS] query datasourceId={}, sqlPreview={}...", datasourceId, sqlPreview);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        log.info("[DS] query result rows={}", rows == null ? 0 : rows.size());
        return rows;
    }
}



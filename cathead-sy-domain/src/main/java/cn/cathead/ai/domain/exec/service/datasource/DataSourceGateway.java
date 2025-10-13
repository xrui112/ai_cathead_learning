package cn.cathead.ai.domain.exec.service.datasource;

import java.util.List;
import java.util.Map;

/**
 * 数据源网关：最小支持 SQL 查询。
 */
public interface DataSourceGateway {

    /**
     * 执行只读 SQL 查询，返回行列表。
     */
    List<Map<String, Object>> query(String datasourceId, String sql);
}



package cn.cathead.ai.infrastructure.viz;

import cn.cathead.ai.domain.exec.service.viz.VizTransformService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimpleVizTransformService implements VizTransformService {

    @Override
    public Map<String, Object> toEcharts(List<Map<String, Object>> rows, JsonNode vizSpec) {
        System.out.println("[VIZ] build option, inRows=" + (rows == null ? 0 : rows.size()) + ", spec=" + vizSpec);
        String chart = vizSpec.hasNonNull("chart") ? vizSpec.get("chart").asText("bar") : "bar";
        String x = vizSpec.hasNonNull("x") ? vizSpec.get("x").asText("") : "";
        String seriesKey = vizSpec.path("series").hasNonNull("key") ? vizSpec.path("series").get("key").asText("") : "";
        String seriesVal = vizSpec.path("series").hasNonNull("value") ? vizSpec.path("series").get("value").asText("") : "";

        // x 轴分类
        LinkedHashSet<String> xSet = new LinkedHashSet<>();
        // 系列名集合
        LinkedHashSet<String> sKeys = new LinkedHashSet<>();
        // (seriesKey, x) -> value
        Map<String, Map<String, Number>> grid = new HashMap<>();

        for (Map<String, Object> r : rows) {
            String xVal = String.valueOf(r.getOrDefault(x, ""));
            String k = String.valueOf(r.getOrDefault(seriesKey, ""));
            Number v = toNumber(r.get(seriesVal));
            xSet.add(xVal);
            sKeys.add(k);
            grid.computeIfAbsent(k, key -> new HashMap<>()).put(xVal, v);
        }

        List<String> xAxis = new ArrayList<>(xSet);
        List<Map<String, Object>> series = new ArrayList<>();
        for (String k : sKeys) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", k);
            s.put("type", chart);
            List<Number> data = new ArrayList<>(xAxis.size());
            Map<String, Number> row = grid.getOrDefault(k, Map.of());
            for (String xv : xAxis) data.add(row.getOrDefault(xv, 0));
            s.put("data", data);
            series.add(s);
        }

        Map<String, Object> option = new LinkedHashMap<>();
        option.put("xAxis", Map.of("type", "category", "data", xAxis));
        option.put("yAxis", Map.of("type", "value"));
        option.put("series", series);
        System.out.println("[VIZ] option keys=" + option.keySet());
        return option;
    }

    private Number toNumber(Object v) {
        if (v instanceof Number n) return n;
        if (v == null) return 0;
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0; }
    }
}



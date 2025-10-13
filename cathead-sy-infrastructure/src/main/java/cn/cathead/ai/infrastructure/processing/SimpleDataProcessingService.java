package cn.cathead.ai.infrastructure.processing;

import cn.cathead.ai.domain.exec.service.processing.DataProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimpleDataProcessingService implements DataProcessingService {

    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> rows, JsonNode opsArray) {
        if (rows == null) rows = List.of();
        if (opsArray == null || !opsArray.isArray() || opsArray.size() == 0) return rows;

        List<Map<String, Object>> current = new ArrayList<>(rows);
        System.out.println("[PROC] start, rows=" + current.size());
        for (JsonNode op : opsArray) {
            String name = op.hasNonNull("op") ? op.get("op").asText("") : "";
            switch (name) {
                case "group_by" -> {
                    List<String> keys = new ArrayList<>();
                    if (op.has("keys") && op.get("keys").isArray()) {
                        for (JsonNode k : op.get("keys")) keys.add(k.asText(""));
                    }
                    current = groupByKeys(current, keys);
                    System.out.println("[PROC] group_by keys=" + keys + ", rows=" + current.size());
                }
                case "sum" -> {
                    String field = op.hasNonNull("field") ? op.get("field").asText("") : "";
                    String as = op.hasNonNull("as") ? op.get("as").asText(field) : field;
                    current = sumField(current, field, as);
                    System.out.println("[PROC] sum field=" + field + " as=" + as + ", rows=" + current.size());
                }
                case "sort" -> {
                    List<String> by = new ArrayList<>();
                    if (op.has("by") && op.get("by").isArray()) {
                        for (JsonNode k : op.get("by")) by.add(k.asText(""));
                    }
                    current = sortBy(current, by);
                    System.out.println("[PROC] sort by=" + by + ", rows=" + current.size());
                }
                default -> {
                    // ignore unknown op
                }
            }
        }
        System.out.println("[PROC] end, rows=" + current.size());
        return current;
    }

    private List<Map<String, Object>> groupByKeys(List<Map<String, Object>> rows, List<String> keys) {
        if (keys.isEmpty()) return rows;
        return rows.stream().collect(Collectors.groupingBy(r -> keyTuple(r, keys)))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    for (int i = 0; i < keys.size(); i++) {
                        m.put(keys.get(i), e.getKey()[i]);
                    }
                    // 暂不聚合指标，交给后续 sum
                    m.put("__rows", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> sumField(List<Map<String, Object>> rows, String field, String as) {
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            if (r.containsKey("__rows")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) r.get("__rows");
                double sum = 0.0;
                for (Map<String, Object> c : children) {
                    Object v = c.get(field);
                    if (v instanceof Number n) sum += n.doubleValue();
                    else if (v != null) {
                        try { sum += Double.parseDouble(String.valueOf(v)); } catch (Exception ignore) {}
                    }
                }
                Map<String, Object> m = new LinkedHashMap<>(r);
                m.remove("__rows");
                m.put(as, sum);
                out.add(m);
            } else {
                Map<String, Object> m = new LinkedHashMap<>(r);
                Object v = r.get(field);
                double sum = 0.0;
                if (v instanceof Number n) sum = n.doubleValue();
                else if (v != null) {
                    try { sum = Double.parseDouble(String.valueOf(v)); } catch (Exception ignore) {}
                }
                m.put(as, sum);
                out.add(m);
            }
        }
        return out;
    }

    private List<Map<String, Object>> sortBy(List<Map<String, Object>> rows, List<String> by) {
        if (by.isEmpty()) return rows;
        List<Map<String, Object>> list = new ArrayList<>(rows);
        list.sort(Comparator.comparing(r -> by.stream().map(k -> String.valueOf(r.getOrDefault(k, ""))).collect(Collectors.joining("|"))));
        return list;
    }

    private Object[] keyTuple(Map<String, Object> r, List<String> keys) {
        Object[] t = new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            t[i] = r.get(keys.get(i));
        }
        return t;
    }
}



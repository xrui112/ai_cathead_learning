package cn.cathead.ai.domain.model.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 表单配置
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormConfiguration {
    private String provider;       // 提供商
    private String type;          // 模型类型
    private List<FieldDefinition> fields; // 字段列表
    private List<String> fieldGroups;     // 字段分组
}
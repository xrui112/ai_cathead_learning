package cn.cathead.ai.domain.model.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Getter
public class ChatModelEntity extends BaseModelEntity {
    private Float temperature;
    private Float topP;
    private Integer maxTokens;
    private Float presencePenalty;
    private Float frequencyPenalty;
    private String[] stop;
    
    // 动态属性，存储模型的扩展参数
    private Map<String, Object> dynamicProperties;
}

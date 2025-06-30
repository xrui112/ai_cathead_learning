package cn.cathead.ai.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ChatModelDTO extends BaseModelDTO {
    private Float temperature;
    private Float topP;
    private Integer maxTokens;
    private String[] stop;
    private Float frequencyPenalty;
    private Float presencePenalty;
}

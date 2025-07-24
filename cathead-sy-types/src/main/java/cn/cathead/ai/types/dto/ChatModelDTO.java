package cn.cathead.ai.types.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatModelDTO extends BaseModelDTO {
    private Float temperature;
    private Float topP;
    private Integer maxTokens;
    private String[] stop;
    private Float frequencyPenalty;
    private Float presencePenalty;
}

package cn.cathead.ai.infrastructure.persistent.mcpserver.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
/**
 * 非原创desuwa
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComputerFunctionRequest {

    @JsonProperty(required = true, value = "computer")
    @JsonPropertyDescription("电脑名称")
    private String computer;

}

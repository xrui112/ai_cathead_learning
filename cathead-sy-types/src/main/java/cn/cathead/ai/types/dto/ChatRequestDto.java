package cn.cathead.ai.types.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequestDto implements Serializable {

    private String modelId;

    private String prompt;

}

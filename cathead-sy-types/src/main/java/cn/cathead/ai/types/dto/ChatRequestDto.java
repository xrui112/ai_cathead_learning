package cn.cathead.ai.types.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatRequestDTO implements Serializable {

    private String modelId;

    private String prompt;

} 
package cn.cathead.ai.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS_CREATE("0000", "创建成功"),
    FAILED_CREATE("0001","重复模型创建")
    ;



    private String code;
    private String info;

}

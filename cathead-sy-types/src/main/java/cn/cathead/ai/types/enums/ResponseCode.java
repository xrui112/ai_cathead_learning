package cn.cathead.ai.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS_CREATE("0000", "创建成功"),
    FAILED_CREATE("0001","重复模型创建"),
    OPTIMISTIC_LOCK_FAILED("0002", "配置更新失败，数据已被其他用户修改，请重试"),
    ILLEGAL_ARGUMENT("0003", "参数错误"),
    ;



    private String code;
    private String info;

}

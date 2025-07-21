package cn.cathead.ai.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    // 通用成功响应
    SUCCESS("0000", "操作成功"),
    
    // 通用失败响应
    FAILED("0001", "操作失败"),
    
    // 模型创建相关
    SUCCESS_CREATE("0000", "创建成功"),
    FAILED_CREATE("0001","重复模型创建"),
    
    // 乐观锁相关
    OPTIMISTIC_LOCK_FAILED("0002", "配置更新失败，数据已被其他用户修改，请重试"),
    
    // 参数相关
    ILLEGAL_ARGUMENT("0003", "参数错误"),
    
    // 模型配置更新相关
    SUCCESS_UPDATE_CHAT("0000", "Chat模型配置更新成功"),
    FAILED_UPDATE_CHAT("0001", "更新Chat模型配置失败"),
    SUCCESS_UPDATE_EMBEDDING("0000", "Embedding模型配置更新成功"),
    FAILED_UPDATE_EMBEDDING("0001", "更新Embedding模型配置失败"),
    
    // 模型删除相关
    SUCCESS_DELETE("0000", "模型删除成功"),
    FAILED_DELETE("0001", "删除模型失败"),
    
    // 模型查询相关
    SUCCESS_GET_MODEL("0000", "获取模型信息成功"),
    MODEL_NOT_FOUND("0001", "模型不存在"),
    FAILED_GET_MODEL("0001", "获取模型信息失败"),
    
    // 模型缓存相关
    SUCCESS_REFRESH_CACHE("0000", "模型缓存刷新成功"),
    FAILED_REFRESH_CACHE("0001", "刷新模型缓存失败"),
    SUCCESS_REFRESH_ALL_CACHE("0000", "批量刷新模型缓存成功"),
    FAILED_REFRESH_ALL_CACHE("0001", "批量刷新模型缓存失败"),
    
    // Bean管理相关
    SUCCESS_GET_BEAN_STATS("0000", "获取Bean管理统计信息成功"),
    FAILED_GET_BEAN_STATS("0001", "获取Bean管理统计信息失败"),
    SUCCESS_CLEAR_BEANS("0000", "清空所有模型Bean成功"),
    FAILED_CLEAR_BEANS("0001", "清空所有模型Bean失败"),
    
    // 动态表单相关
    SUCCESS_GET_FORM_CONFIG("00004", "获取表单配置成功"),
    UNSUPPORTED_PROVIDER_TYPE("0005", "不支持的提供商或类型"),
    FAILED_GET_FORM_CONFIG("0006", "获取表单配置失败"),
    SUCCESS_VALIDATE_FORM("0007", "表单数据校验通过"),
    FAILED_VALIDATE_FORM("0008", "表单数据校验失败"),
    SUCCESS_SUBMIT_FORM("0009", "表单提交成功"),
    FAILED_SUBMIT_FORM("0010", "提交表单失败");

    private String code;
    private String info;
}

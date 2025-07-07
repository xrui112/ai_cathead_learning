package cn.cathead.ai.trigger.http;

import cn.cathead.ai.types.enums.ResponseCode;
import cn.cathead.ai.types.exception.OptimisticLockException;
import cn.cathead.ai.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理乐观锁冲突异常
     */
    @ExceptionHandler(OptimisticLockException.class)
    public Response<String> handleOptimisticLockException(OptimisticLockException e) {
        log.warn("乐观锁冲突: {}", e.getMessage());
        return Response.<String>builder()
                .code(ResponseCode.OPTIMISTIC_LOCK_FAILED.getCode())
                .info(ResponseCode.OPTIMISTIC_LOCK_FAILED.getInfo())
                .data(null)
                .build();
    }
    
    /**
     * 处理参数错误异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return Response.<String>builder()
                .code(ResponseCode.ILLEGAL_ARGUMENT.getCode())
                .info(ResponseCode.ILLEGAL_ARGUMENT.getInfo() + "：" + e.getMessage())
                .data(null)
                .build();
    }
}

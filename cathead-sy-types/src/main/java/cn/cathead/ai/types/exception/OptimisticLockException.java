package cn.cathead.ai.types.exception;

/**
 * 乐观锁冲突异常
 */
public class OptimisticLockException extends RuntimeException {
    
    public OptimisticLockException(String message) {
        super(message);
    }
    
    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
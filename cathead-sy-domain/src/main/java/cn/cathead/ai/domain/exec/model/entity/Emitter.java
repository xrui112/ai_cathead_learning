package cn.cathead.ai.domain.exec.model.entity;

/**
 * 轻量级 SSE 发射器抽象，使用泛型以支持结构化事件。
 */
public interface Emitter<T> {
    void send(T data) throws Exception;
    void complete();
}



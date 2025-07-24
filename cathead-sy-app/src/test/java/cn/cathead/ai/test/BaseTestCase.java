package cn.cathead.ai.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 基础测试类，所有测试类都应继承此类
 */
@SpringBootTest
@ActiveProfiles("dev")
public abstract class BaseTestCase {

    @BeforeEach
    public void setUp() {
        // 在每个测试方法执行前进行初始化
        setupTestData();
    }

    /**
     * 设置测试数据，子类可以重写此方法
     */
    protected void setupTestData() {
        // 默认实现为空，子类可以重写
    }

    /**
     * 清理测试数据，子类可以重写此方法
     */
    protected void cleanupTestData() {
        // 默认实现为空，子类可以重写
    }
} 
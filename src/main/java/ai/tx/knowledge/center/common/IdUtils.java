package ai.tx.knowledge.center.common;

import lombok.extern.slf4j.Slf4j;

/**
 * ID生成工具类
 * 提供统一的ID生成方法，基于雪花算法实现
 * 
 * @author tanxiong
 * @date 2025/01/21
 */
@Slf4j
public class IdUtils {
    
    /**
     * 雪花算法ID生成器实例（单例）
     * 使用类加载时初始化，保证线程安全
     */
    private static final SnowflakeIdGenerator SNOWFLAKE_GENERATOR = new SnowflakeIdGenerator(1L, 1L);
    
    /**
     * 私有构造函数，防止实例化
     */
    private IdUtils() {
        throw new UnsupportedOperationException("IdUtils is a utility class and cannot be instantiated");
    }
    
    /**
     * 生成字符串格式的唯一ID
     * 
     * @return 唯一ID字符串
     */
    public static String generateId() {
        return SNOWFLAKE_GENERATOR.nextStringId();
    }
    
    /**
     * 生成长整型格式的唯一ID
     * 
     * @return 唯一ID长整型
     */
    public static Long generateLongId() {
        return SNOWFLAKE_GENERATOR.nextId();
    }
    
    /**
     * 解析雪花ID的各个组成部分
     * 
     * @param id 雪花ID
     * @return ID信息
     */
    public static SnowflakeIdGenerator.SnowflakeIdInfo parseId(long id) {
        return SNOWFLAKE_GENERATOR.parseId(id);
    }
    
    /**
     * 解析字符串格式的雪花ID
     * 
     * @param id 字符串格式的雪花ID
     * @return ID信息
     */
    public static SnowflakeIdGenerator.SnowflakeIdInfo parseId(String id) {
        try {
            long longId = Long.parseLong(id);
            return parseId(longId);
        } catch (NumberFormatException e) {
            log.warn("无法解析ID: {}", id, e);
            throw new IllegalArgumentException("无效的ID格式: " + id);
        }
    }
} 
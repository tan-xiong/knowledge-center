package ai.tx.knowledge.center.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 * 生成64位唯一ID：1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
 * 
 * @author tanxiong
 * @date 2025/01/21
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {

    /**
     * 起始时间戳 (2025-01-01 00:00:00)
     */
    private static final long START_TIMESTAMP = 1735689600000L;

    /**
     * 机器ID所占的位数
     */
    private static final long WORKER_ID_BITS = 5L;
    
    /**
     * 数据中心ID所占的位数
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 支持的最大机器ID (0-31)
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    
    /**
     * 支持的最大数据中心ID (0-31)
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列在ID中占的位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    
    /**
     * 数据中心ID左移位数
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 生成序列的掩码 (4095)
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 工作机器ID (0-31)
     */
    private final long workerId;
    
    /**
     * 数据中心ID (0-31)
     */
    private final long datacenterId;

    /**
     * 毫秒内序列 (0-4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     */
    public SnowflakeIdGenerator() {
        // 默认配置：workerId=1, datacenterId=1
        this(1L, 1L);
    }

    /**
     * 构造函数
     * @param workerId 工作机器ID
     * @param datacenterId 数据中心ID
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("Worker ID 必须在 0 到 %d 之间", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("Datacenter ID 必须在 0 到 %d 之间", MAX_DATACENTER_ID));
        }
        
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        
        log.info("雪花算法ID生成器初始化完成: workerId={}, datacenterId={}", workerId, datacenterId);
    }

    /**
     * 获得下一个ID (线程安全)
     * @return 64位唯一ID
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                String.format("时钟回退异常，拒绝生成ID。当前时间戳: %d, 上次时间戳: %d", 
                    timestamp, lastTimestamp));
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }

        // 上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成字符串格式的ID
     * @return 字符串ID
     */
    public String nextStringId() {
        return String.valueOf(nextId());
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 返回当前时间，以毫秒为单位
     * @return 当前时间(毫秒)
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 解析雪花ID的各个组成部分
     * @param id 雪花ID
     * @return ID信息
     */
    public SnowflakeIdInfo parseId(long id) {
        long timestamp = (id >> TIMESTAMP_LEFT_SHIFT) + START_TIMESTAMP;
        long datacenterId = (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
        long workerId = (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        long sequence = id & SEQUENCE_MASK;
        
        return new SnowflakeIdInfo(timestamp, datacenterId, workerId, sequence);
    }

    /**
     * 雪花ID信息
     */
    public static class SnowflakeIdInfo {
        private final long timestamp;
        private final long datacenterId;
        private final long workerId;
        private final long sequence;

        public SnowflakeIdInfo(long timestamp, long datacenterId, long workerId, long sequence) {
            this.timestamp = timestamp;
            this.datacenterId = datacenterId;
            this.workerId = workerId;
            this.sequence = sequence;
        }

        public long getTimestamp() { return timestamp; }
        public long getDatacenterId() { return datacenterId; }
        public long getWorkerId() { return workerId; }
        public long getSequence() { return sequence; }

        @Override
        public String toString() {
            return String.format("SnowflakeIdInfo{timestamp=%d, datacenterId=%d, workerId=%d, sequence=%d}", 
                timestamp, datacenterId, workerId, sequence);
        }
    }
} 
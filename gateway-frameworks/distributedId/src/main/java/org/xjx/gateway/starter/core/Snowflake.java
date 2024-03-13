package org.xjx.gateway.starter.core;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import org.xjx.gateway.starter.core.snowflake.SnowflakeIdInfo;

import java.io.Serializable;
import java.util.Date;

/**
 * 雪花算法
 */

public class Snowflake implements Serializable, IdGenerator {
    /**
     * 默认起始时间
     */
    private static long DEFAULT_TWEPOCH = 1288834974657L;
    /**
     * 默认回拨时间
     */
    private static long DEFAULT_TIME_OFFSET = 2000L;
    /**
     * 序列号长度
     */
    private static long SEQUENCE_BITS = 12L;
    /**
     * 数据中心、机器编号长度
     */
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_BITS = 5L;
    /**
     * 时间戳长度
     */
    private static final long TIMEOFFSET_BITS = 41L;
    /**
     * 最大机器节点数
     */
    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
    /**
     * 最大数据中心节点数
     */
    private static final long MAX_DATACENTER_ID = -1 ^ (-1L << DATACENTER_BITS);
    /**
     * 最大序列号数
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    /**
     * 偏移量
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_BITS;

    /**
     * 参数
     */
    private final long twepoch;
    private final long workerId;
    private final long dataCenterId;
    private final long timeOffsets;
    private final boolean useSystemClock;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake() {
        this(IdUtil.getWorkerId(IdUtil.getDataCenterId(MAX_DATACENTER_ID), MAX_WORKER_ID));
    }

    public Snowflake(long workerId) {
        this(workerId, IdUtil.getDataCenterId(MAX_DATACENTER_ID));
    }

    public Snowflake(long workerId, long dataCenterId) {
        this(null, workerId, dataCenterId, false, 0);
    }

    public Snowflake(long workerId, long dataCenterId, boolean isUseSystemLock) {
        this(null, workerId, dataCenterId, isUseSystemLock, 0);
    }

    public Snowflake(Date epochDate, long workerId, long dataCenterId, boolean isUseSystemLock) {
        this(epochDate, workerId, dataCenterId, isUseSystemLock, 0);
    }

    public Snowflake(Date epochDate, long workerId, long dataCenterId, boolean isUseSystemLock, long timeOffset) {
        this.twepoch = epochDate == null ? DEFAULT_TWEPOCH : epochDate.getTime();
        this.workerId = Assert.checkBetween(workerId, 0, MAX_WORKER_ID);
        this.dataCenterId = Assert.checkBetween(dataCenterId, 0, MAX_DATACENTER_ID);
        this.useSystemClock = isUseSystemLock;
        this.timeOffsets = timeOffset;
    }

    public long getWorkerId(long id) {
        return id >> WORKER_ID_SHIFT & ~(-1L << WORKER_ID_BITS);
    }

    public long getDataCenterId(long id) {
        return id >> DATACENTER_ID_SHIFT & ~(-1L << DATACENTER_BITS);
    }

    public synchronized long nextId() {
        long timeStamp = genTime();
        // 发生时钟回拨
        if (timeStamp < this.lastTimestamp) {
            if (this.lastTimestamp - timeStamp < timeOffsets) {
                timeStamp = lastTimestamp;
            } else {
                throw new RuntimeException("Clock moved callbacks!");
            }
        }
        // 相同时间，生成序列号不同
        if (timeStamp == this.lastTimestamp) {
            final long sequence = (this.sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timeStamp = waitNextTime(this.lastTimestamp);
            }
            this.sequence = sequence;
        } else {
            sequence = RandomUtil.randomLong();
        }
        lastTimestamp = timeStamp;
        return ((lastTimestamp - twepoch) << TIMESTAMP_SHIFT) | (dataCenterId << DATACENTER_ID_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    private long waitNextTime(long lastTimestamp) {
        long currentTime = genTime();
        while (currentTime == lastTimestamp) {
            currentTime = genTime();
        }
        if (currentTime < lastTimestamp) {
            throw new RuntimeException("Clock moved callbacks!");
        }
        return currentTime;
    }

    public long genTime() {
        return this.useSystemClock ? SystemClock.now() : System.currentTimeMillis();
    }

    public SnowflakeIdInfo parseSnowflakeId(long snowflakeId) {
        SnowflakeIdInfo info = SnowflakeIdInfo.builder().sequence((int)(snowflakeId & ~(-1L << SEQUENCE_BITS)))
                .workerId((int)((snowflakeId >> WORKER_ID_SHIFT) & ~(-1L << WORKER_ID_BITS)))
                .dataCenterId((int)((snowflakeId >> DATACENTER_ID_SHIFT) & ~(-1L << DATACENTER_BITS)))
                .timestamp((snowflakeId >> TIMESTAMP_SHIFT)+twepoch).build();
        return info;
    }
}

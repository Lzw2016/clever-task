package org.clever.task.core.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度器集群锁(SchedulerLock)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:33
 */
@Data
public class SchedulerLock implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createAt;

    /**
     * 更新时间
     */
    private Date updateAt;
}

package org.clever.task.core.entity;

import java.util.Date;
import java.io.Serializable;

import lombok.Data;

/**
 * 任务触发器日志(JobTriggerLog)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:32
 */
@Data
public class JobTriggerLog implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 调度器实例名称
     */
    private String instanceName;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 触发器名称
     */
    private String triggerName;

    /**
     * 是否是手动触发，0：系统自动触发，1：用户手动触发
     */
    private Integer isManual;

    /**
     * 触发耗时(单位：毫秒)
     */
    private Integer triggerTime;

    /**
     * 上一次触发时间
     */
    private Date lastFireTime;

    /**
     * 下一次触发时间
     */
    private Date nextFireTime;

    /**
     * 触发次数
     */
    private Integer runCount;

    /**
     * 是否错过了触发，0：否，1：是
     */
    private Integer misFired;

    /**
     * 触发器消息
     */
    private String triggerMsg;

    /**
     * 创建时间
     */
    private Date createAt;
}

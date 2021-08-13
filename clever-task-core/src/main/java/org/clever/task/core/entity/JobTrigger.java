package org.clever.task.core.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务触发器(JobTrigger)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:32
 */
@Data
public class JobTrigger implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 触发器名称
     */
    private String name;

    /**
     * 触发开始时间
     */
    private Date startTime;

    /**
     * 触发结束时间
     */
    private Date endTime;

    /**
     * 上一次触发时间
     */
    private Date lastFireTime;

    /**
     * 下一次触发时间
     */
    private Date nextFireTime;

    /**
     * 错过触发策略，1：忽略，2：立即补偿触发一次
     */
    private Integer misfireStrategy;

    /**
     * 触发器状态，0：停止，1：触发中
     */
    private Integer state;

    /**
     * 任务类型，1：cron触发，2：固定速率触发
     */
    private Integer type;

    /**
     * cron表达式
     */
    private String cron;

    /**
     * 固定速率触发，间隔时间(单位：秒)
     */
    private Long fixedInterval;

    /**
     * 是否禁用：0-启用，1-禁用
     */
    private Integer disable;

    /**
     * 描述
     */
    private String description;

    /**
     * 乐观锁
     */
    private Long lockVersion;

    /**
     * 创建时间
     */
    private Date createAt;

    /**
     * 更新时间
     */
    private Date updateAt;
}

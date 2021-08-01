package org.clever.task.core.entity;

import java.util.Date;
import java.io.Serializable;

import lombok.Data;

/**
 * 任务执行日志(JobLog)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:32
 */
@Data
public class JobLog implements Serializable {
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
     * 开始执行时间
     */
    private Date startTime;

    /**
     * 执行结束时间
     */
    private Date endTime;

    /**
     * 执行耗时(单位：毫秒)
     */
    private Integer runTime;

    /**
     * 任务执行结果，0：成功，1：失败
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 异常信息
     */
    private String exceptionInfo;

    /**
     * 执行次数
     */
    private Integer runCount;

    /**
     * 执行前的任务数据
     */
    private String beforeJobData;

    /**
     * 执行后的任务数据
     */
    private String afterJobData;

    /**
     * 创建时间
     */
    private Date createAt;
}

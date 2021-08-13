package org.clever.task.core.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务执行日志(JobLog)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:32
 */
@Data
public class JobLog implements Serializable {
    public static final int EXCEPTION_INFO_MAX_LENGTH = 2046;

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
     * 对应的触发器日志ID
     */
    private Long jobTriggerLogId;

    /**
     * 任务触发器ID
     */
    private Long jobTriggerId;

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
     * 任务执行结果，0：成功，1：失败，2：取消
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
    private Long runCount;

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

    public void setExceptionInfo(String exceptionInfo) {
        this.exceptionInfo = StringUtils.truncate(exceptionInfo, EXCEPTION_INFO_MAX_LENGTH);
    }
}

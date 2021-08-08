package org.clever.task.core.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调度器事件日志(SchedulerLog)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:33
 */
@Data
public class SchedulerLog implements Serializable {
    /**
     * 编号
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
     * 事件名称
     */
    private String eventName;

    /**
     * 事件日志数据
     */
    private String logData;

    /**
     * 创建时间
     */
    private Date createAt;
}
package org.clever.task.core.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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
    public static final int LOG_DATA_MAX_LENGTH = 32767;
    /**
     * 数据完整性校验失败
     */
    public static final String EVENT_DATA_CHECK_ERROR = "data_check_error";
    /**
     * 校准触发器触发时间失败
     */
    public static final String EVENT_CALC_NEXT_FIRE_TIME_ERROR = "calc_next_fire_time_error";
    /**
     * 校准触发器触发时间过程中，计算cron表达式下次触发时间失败
     */
    public static final String EVENT_CALC_Cron_NEXT_FIRE_TIME_ERROR = "calc_cron_next_fire_time_error";

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

    public void setEventInfo(String eventName, String logData) {
        this.eventName = eventName;
        this.logData = StringUtils.truncate(logData, LOG_DATA_MAX_LENGTH);
    }
}

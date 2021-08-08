package org.clever.task.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.entity.JobTrigger;
import org.clever.task.core.exception.SchedulerException;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/03 16:09 <br/>
 */
public class JobTriggerUtils {
    /**
     * 计算下一次触发时间(返回时间可能小于now)
     *
     * @param jobTrigger 触发器配置
     * @return 大于等于now：等待正常触发，<br/>小于now：需要补偿触发，<br/>大于endTime：触发器已结束
     */
    public static Date getNextFireTime(final JobTrigger jobTrigger) {
        if (jobTrigger.getStartTime() == null) {
            throw new SchedulerException(String.format("任务触发器startTime字段不能为空，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        if (jobTrigger.getType() == null) {
            throw new SchedulerException(String.format("任务触发器type字段不能为空，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        // 计算起始时间 -> max(jobTrigger.getLastFireTime(), jobTrigger.getStartTime())
        Date calcStartTime = jobTrigger.getLastFireTime() != null ? jobTrigger.getLastFireTime() : jobTrigger.getStartTime();
        if (jobTrigger.getLastFireTime() != null && jobTrigger.getStartTime().compareTo(jobTrigger.getLastFireTime()) > 0) {
            calcStartTime = jobTrigger.getStartTime();
        }
        // 计算下一次触发时间
        Date nextFireTime;
        switch (jobTrigger.getType()) {
            case EnumConstant.JOB_TRIGGER_TYPE_1:
                // cron触发
                if (StringUtils.isBlank(jobTrigger.getCron())) {
                    throw new SchedulerException(String.format("任务触发器cron字段不能为空，JobTrigger(id=%s)", jobTrigger.getId()));
                }
                try {
                    nextFireTime = CronExpressionUtil.getNextTime(jobTrigger.getCron(), calcStartTime);
                } catch (Exception e) {
                    throw new SchedulerException(String.format("任务触发器cron字段值错误，JobTrigger(id=%s)", jobTrigger.getId()), e);
                }
                break;
            case EnumConstant.JOB_TRIGGER_TYPE_2:
                // 固定速率触发
                if (jobTrigger.getFixedInterval() == null || jobTrigger.getFixedInterval() <= 0) {
                    throw new SchedulerException(String.format("任务触发器fixedInterval字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
                }
                nextFireTime = new Date(calcStartTime.getTime() + (jobTrigger.getFixedInterval() * 1000));
                break;
            case EnumConstant.JOB_TRIGGER_TYPE_3:
                // 固定延时触发 TODO 暂不支持固定延时触发
                if (jobTrigger.getDelayTime() == null || jobTrigger.getDelayTime() <= 0) {
                    throw new SchedulerException(String.format("任务触发器delayTime字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
                }
                throw new SchedulerException(String.format("暂不支持固定延时触发，JobTrigger(id=%s)", jobTrigger.getId()));
            default:
                throw new SchedulerException(String.format("任务触发器type字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        // 只需要精确到秒
        if (nextFireTime != null && nextFireTime.getTime() % 1000 != 0) {
            nextFireTime = new Date(nextFireTime.getTime() - (nextFireTime.getTime() % 1000));
        }
        return nextFireTime;
    }

    /**
     * 计算下一次触发时间
     *
     * @param dbNow      数据库当前时间
     * @param jobTrigger 触发器配置
     * @return 等于null：已经结束，<br/>
     */
    public static Date getNextFireTime(final Date dbNow, final JobTrigger jobTrigger) {
        if (jobTrigger.getType() == null) {
            throw new SchedulerException(String.format("任务触发器type字段不能为空，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        if (jobTrigger.getNextFireTime() == null) {
            throw new SchedulerException(String.format("任务触发器nextFireTime字段不能为空，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        Date nextFireTime;
        switch (jobTrigger.getType()) {
            case EnumConstant.JOB_TRIGGER_TYPE_1:
                // cron触发
                if (StringUtils.isBlank(jobTrigger.getCron())) {
                    throw new SchedulerException(String.format("任务触发器cron字段不能为空，JobTrigger(id=%s)", jobTrigger.getId()));
                }
                try {
                    nextFireTime = CronExpressionUtil.getNextTime(jobTrigger.getCron(), dbNow);
                } catch (Exception e) {
                    throw new SchedulerException(String.format("任务触发器cron字段值错误，JobTrigger(id=%s)", jobTrigger.getId()), e);
                }
                break;
            case EnumConstant.JOB_TRIGGER_TYPE_2:
                // 固定速率触发
                if (jobTrigger.getFixedInterval() == null || jobTrigger.getFixedInterval() <= 0) {
                    throw new SchedulerException(String.format("任务触发器fixedInterval字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
                }
                nextFireTime = dbNow.compareTo(jobTrigger.getNextFireTime()) > 0 ? dbNow : jobTrigger.getNextFireTime();
                nextFireTime = new Date(nextFireTime.getTime() + (jobTrigger.getFixedInterval() * 1000));
                break;
            case EnumConstant.JOB_TRIGGER_TYPE_3:
                // 固定延时触发 TODO 暂不支持固定延时触发
                if (jobTrigger.getDelayTime() == null || jobTrigger.getDelayTime() <= 0) {
                    throw new SchedulerException(String.format("任务触发器delayTime字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
                }
                throw new SchedulerException(String.format("暂不支持固定延时触发，JobTrigger(id=%s)", jobTrigger.getId()));
            default:
                throw new SchedulerException(String.format("任务触发器type字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        if (jobTrigger.getEndTime() != null && nextFireTime.compareTo(jobTrigger.getEndTime()) > 0) {
            nextFireTime = null;
        }
        // 只需要精确到秒
        if (nextFireTime != null && nextFireTime.getTime() % 1000 != 0) {
            nextFireTime = new Date(nextFireTime.getTime() - (nextFireTime.getTime() % 1000));
        }
        return nextFireTime;
    }
}
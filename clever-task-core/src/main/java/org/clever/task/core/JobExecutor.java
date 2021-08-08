package org.clever.task.core;

import org.clever.task.core.entity.Job;

import java.util.Date;

/**
 * 定时任务执行器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 23:52 <br/>
 */
public interface JobExecutor {
    /**
     * 是否支持执行任务类型
     */
    boolean support(int jobType);

    /**
     * 定时任务执行器排序
     */
    int order();

    /**
     * 执行定时任务实现
     *
     * @param dbNow 数据库当前时间
     * @param job   任务信息
     */
    void exec(final Date dbNow, final Job job);
}

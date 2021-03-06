package org.clever.task.core;

import org.clever.task.core.entity.Job;
import org.clever.task.core.entity.Scheduler;

import java.util.Date;

/**
 * 定时任务执行器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 23:52 <br/>
 */
public interface JobExecutor {
    /**
     * 是否支持执行任务类型 (1：http调用，2：java调用，3：js脚本，4：shell脚本)
     */
    boolean support(int jobType);

    /**
     * 定时任务执行器优先级(值越小越优先)
     */
    int order();

    /**
     * 执行定时任务实现
     *
     * @param dbNow 数据库当前时间
     * @param job   任务信息
     */
    void exec(final Date dbNow, final Job job, final Scheduler scheduler, final TaskStore taskStore) throws Exception;
}

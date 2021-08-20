package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.JobLog;
import org.clever.task.core.entity.Scheduler;

/**
 * 定时任务执行事件监听器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:49 <br/>
 */
public interface JobListener {
    /**
     * 开始执行
     */
    void onStartRun(Scheduler scheduler, TaskStore taskStore, JobLog jobLog);

    /**
     * 执行完成(成功或者失败)
     */
    void onEndRun(Scheduler scheduler, TaskStore taskStore, JobLog jobLog);

    /**
     * 重试执行
     */
    void onRetryRun(Scheduler scheduler, TaskStore taskStore, JobLog jobLog, Exception error);
}

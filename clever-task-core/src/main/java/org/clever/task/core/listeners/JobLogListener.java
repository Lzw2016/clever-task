package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.JobLog;
import org.clever.task.core.entity.Scheduler;

/**
 * 定时任务执行日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 21:02 <br/>
 */
public class JobLogListener implements JobListener {
    @Override
    public void onStartRun(Scheduler scheduler, TaskStore taskStore, JobLog jobLog) {

    }

    @Override
    public void onEndRun(Scheduler scheduler, TaskStore taskStore, JobLog jobLog) {

    }

    @Override
    public void onRetryRun(Scheduler scheduler, TaskStore taskStore, JobLog jobLog) {

    }
}

package org.clever.task.ext.job;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.JobExecutor;
import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.entity.Job;
import org.clever.task.core.entity.Scheduler;
import org.clever.task.core.entity.ShellJob;
import org.clever.task.core.exception.JobExecutorException;

import java.util.Date;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:56 <br/>
 */
@Slf4j
public class ShellJobExecutor implements JobExecutor {
    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_4);
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void exec(Date dbNow, Job job, Scheduler scheduler, TaskStore taskStore) throws Exception {
        final ShellJob shellJob = taskStore.beginReadOnlyTX(status -> taskStore.getShellJob(scheduler.getNamespace(), job.getId()));
        if (shellJob == null) {
            throw new JobExecutorException(String.format("ShellJob数据不存在，JobId=%s", job.getId()));
        }
        // TODO 执行ShellJob
    }
}

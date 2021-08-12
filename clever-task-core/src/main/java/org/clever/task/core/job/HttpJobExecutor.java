package org.clever.task.core.job;

import org.clever.task.core.JobExecutor;
import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.entity.HttpJob;
import org.clever.task.core.entity.Job;
import org.clever.task.core.entity.Scheduler;

import java.util.Date;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/12 12:24 <br/>
 */
public class HttpJobExecutor implements JobExecutor {
    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_1);
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void exec(Date dbNow, Job job, Scheduler scheduler, TaskStore taskStore) {
        HttpJob httpJob = taskStore.beginReadOnlyTX(status -> taskStore.getHttpJob(scheduler.getNamespace(), job.getId()));
        // TODO 发送Http请求
    }
}

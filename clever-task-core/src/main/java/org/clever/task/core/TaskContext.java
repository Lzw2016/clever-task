package org.clever.task.core;

import lombok.Getter;
import lombok.Setter;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.entity.Job;
import org.clever.task.core.entity.JobTrigger;
import org.clever.task.core.entity.Scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 定时任务调度器上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:55 <br/>
 */
public class TaskContext {
//    /**
//     * 对应的调度器实例
//     */
//    @Getter
//    private final TaskInstance schedulerInstance;
    /**
     * 当前调度器配置
     */
    @Getter
    private final SchedulerConfig schedulerConfig;
    /**
     * 当前调度器信息
     */
    @Getter
    private final Scheduler currentScheduler;
    /**
     * 当前节点任务运行的数量计数 {@code ConcurrentMap<jobId, jobRunCount>}
     */
    private final ConcurrentMap<Long, AtomicInteger> jobRunCount = new ConcurrentHashMap<>();
    /**
     * 当前集群可用的调度器列表
     */
    @Getter
    @Setter
    private volatile List<Scheduler> availableSchedulerList;
    /**
     * 接下来N秒内需要触发的触发器列表(N = heartbeatInterval * NEXT_TRIGGER_INTERVAL) {@code ConcurrentMap<jobTriggerId, JobTrigger>}
     */
    private volatile ConcurrentMap<Long, JobTrigger> nextJobTriggerMap;
    /**
     * 当前集群所有定时任务信息 {@code ConcurrentMap<jobId, Job>}
     */
    private volatile ConcurrentMap<Long, Job> allJobMap;
    /**
     * allJobMap数据最后的加载时间
     */
    @Getter
    private volatile long jobLastLoadTime = -1;

    public TaskContext(SchedulerConfig schedulerConfig, Scheduler scheduler) {
//        this.schedulerInstance = schedulerInstance;
        this.schedulerConfig = schedulerConfig;
        this.currentScheduler = scheduler;
    }

    public void setNextJobTriggerMap(List<JobTrigger> nextJobTriggerList) {
        ConcurrentMap<Long, JobTrigger> nextJobTriggerMap = new ConcurrentHashMap<>(nextJobTriggerList.size());
        nextJobTriggerList.forEach(jobTrigger -> nextJobTriggerMap.put(jobTrigger.getId(), jobTrigger));
        this.nextJobTriggerMap = nextJobTriggerMap;
    }

    public void setAllJobMap(List<Job> allJobList, long jobLastLoadTime) {
        ConcurrentMap<Long, Job> allJobMap = new ConcurrentHashMap<>(allJobList.size());
        allJobList.forEach(job -> allJobMap.put(job.getId(), job));
        this.jobLastLoadTime = jobLastLoadTime;
        this.allJobMap = allJobMap;
    }

    public List<JobTrigger> getNextJobTriggerList() {
        if (nextJobTriggerMap == null) {
            return new ArrayList<>();
        }
        return nextJobTriggerMap.values().stream().sorted(Comparator.comparing(JobTrigger::getNextFireTime)).collect(Collectors.toList());
    }

    public void removeNextJobTrigger(long jobTriggerId) {
        nextJobTriggerMap.remove(jobTriggerId);
    }

    public void putNextJobTrigger(JobTrigger jobTrigger) {
        nextJobTriggerMap.put(jobTrigger.getId(), jobTrigger);
    }

    public int getJobRunCount(Long jobId) {
        return jobRunCount.computeIfAbsent(jobId, id -> new AtomicInteger(0)).get();
    }

    public Job getJob(Long jobId) {
        if (allJobMap == null) {
            return null;
        }
        return allJobMap.get(jobId);
    }

    public int getAllJobSize() {
        if (allJobMap == null) {
            return 0;
        }
        return allJobMap.size();
    }
}

package org.clever.task.core;

import lombok.Getter;
import lombok.Setter;
import org.clever.task.core.config.SchedulerConfig;
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
     * 当前节点任务运行的数量计数 {@code ConcurrentMap<jobId, jobRunCount>}
     */
    private final ConcurrentMap<Long, AtomicInteger> jobRunCountMap = new ConcurrentHashMap<>();

    public TaskContext(SchedulerConfig schedulerConfig, Scheduler scheduler) {
        this.schedulerConfig = schedulerConfig;
        this.currentScheduler = scheduler;
    }

    public void setNextJobTriggerMap(List<JobTrigger> nextJobTriggerList) {
        ConcurrentMap<Long, JobTrigger> nextJobTriggerMap = new ConcurrentHashMap<>(nextJobTriggerList.size());
        nextJobTriggerList.forEach(jobTrigger -> nextJobTriggerMap.put(jobTrigger.getId(), jobTrigger));
        this.nextJobTriggerMap = nextJobTriggerMap;
    }

    public List<JobTrigger> getNextJobTriggerList() {
        if (nextJobTriggerMap == null) {
            return new ArrayList<>();
        }
        return nextJobTriggerMap.values().stream().sorted(Comparator.comparing(JobTrigger::getNextFireTime)).collect(Collectors.toList());
    }

    public void removeNextJobTrigger(Long jobTriggerId) {
        nextJobTriggerMap.remove(jobTriggerId);
    }

    public void putNextJobTrigger(JobTrigger jobTrigger) {
        nextJobTriggerMap.put(jobTrigger.getId(), jobTrigger);
    }

    public int getJobRunCount(Long jobId) {
        return jobRunCountMap.computeIfAbsent(jobId, id -> new AtomicInteger(0)).get();
    }

    public int incrementJobRunCount(Long jobId) {
        return jobRunCountMap.computeIfAbsent(jobId, id -> new AtomicInteger(0)).incrementAndGet();
    }

    public int decrementJobRunCount(Long jobId) {
        return jobRunCountMap.computeIfAbsent(jobId, id -> new AtomicInteger(0)).decrementAndGet();
    }

    public void removeJobRunCount(Long jobId) {
        jobRunCountMap.remove(jobId);
    }
}

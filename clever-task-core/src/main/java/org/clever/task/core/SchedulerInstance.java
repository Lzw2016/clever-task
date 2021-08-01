package org.clever.task.core;

/**
 * 调度器实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:38 <br/>
 */
public class SchedulerInstance {

    public String getSchedulerName() {
        return null;
    }

//    String getSchedulerInstanceId()

//    SchedulerContext getContext()

//    void start()

//    void startDelayed(int seconds)

//    boolean isStarted()

//    void standby()

//    boolean isInStandbyMode()

//    void shutdown()

//    void shutdown(boolean waitForJobsToComplete)

//    boolean isShutdown()

//    SchedulerMetaData getMetaData()

//    List<JobExecutionContext> getCurrentlyExecutingJobs()

//    Date scheduleJob(JobDetail jobDetail, Trigger trigger)

//    Date scheduleJob(Trigger trigger)

//    void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace)

//    void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace)

//    boolean unscheduleJob(TriggerKey triggerKey)

//    boolean unscheduleJobs(List<TriggerKey> triggerKeys)

//    Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger)

//    void addJob(JobDetail jobDetail, boolean replace)

//    void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling)

//    boolean deleteJob(JobKey jobKey)

//    boolean deleteJobs(List<JobKey> jobKeys)

//    void triggerJob(JobKey jobKey)

//    void triggerJob(JobKey jobKey, JobDataMap data)

//    void pauseJob(JobKey jobKey)

//    void pauseJobs(GroupMatcher<JobKey> matcher)

//    void pauseTrigger(TriggerKey triggerKey)

//    void pauseTriggers(GroupMatcher<TriggerKey> matcher)

//    void resumeJob(JobKey jobKey)

//    void resumeJobs(GroupMatcher<JobKey> matcher)

//    void resumeTrigger(TriggerKey triggerKey)

//    void resumeTriggers(GroupMatcher<TriggerKey> matcher)

//    void pauseAll()

//    void resumeAll()

//    List<String> getJobGroupNames()

//    Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher)

//    List<? extends Trigger> getTriggersOfJob(JobKey jobKey)

//    List<String> getTriggerGroupNames()

//    Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher)

//    Set<String> getPausedTriggerGroups()

//    JobDetail getJobDetail(JobKey jobKey)

//    Trigger getTrigger(TriggerKey triggerKey)

//    TriggerState getTriggerState(TriggerKey triggerKey)

//    void resetTriggerFromErrorState(TriggerKey triggerKey)

//    void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)

//    boolean deleteCalendar(String calName)

//    Calendar getCalendar(String calName)

//    List<String> getCalendarNames()

//    boolean interrupt(JobKey jobKey)

//    boolean interrupt(String fireInstanceId)

//    boolean checkExists(JobKey jobKey)

//    boolean checkExists(TriggerKey triggerKey)

//    void clear()
}

//package org.clever.task.core;
//
//import org.clever.task.core.entity.Scheduler;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/08/09 22:01 <br/>
// */
//public class TaskService {
//    /**
//     * 调度器数据存储对象
//     */
//    private final TaskStore taskStore;
////    /**
////     * 调度器上下文
////     */
////    private final TaskContext schedulerContext;
//
//    public TaskService(TaskStore taskStore) {
//        this.taskStore = taskStore;
//    }
//
//    /**
//     * 调度器节点注册，返回注册后的调度器对象
//     */
//    public Scheduler registerScheduler(Scheduler scheduler) {
//        return taskStore.beginTX(status -> taskStore.addOrUpdateScheduler(scheduler));
//    }
//
//    /**
//     * 心跳保持
//     */
//    public void heartbeat(Scheduler scheduler) {
//        taskStore.beginTX(status -> taskStore.heartbeat(scheduler));
//    }
//
//
//}

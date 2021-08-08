package org.clever.task.core.entity;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:36 <br/>
 */
public interface EnumConstant {
    /**
     * 任务类型，1：cron触发，2：固定速率触发，3：固定延时触发
     */
    int JOB_TRIGGER_TYPE_1 = 1;
    /**
     * 任务类型，1：cron触发，2：固定速率触发，3：固定延时触发
     */
    int JOB_TRIGGER_TYPE_2 = 2;
    /**
     * 任务类型，1：cron触发，2：固定速率触发，3：固定延时触发
     */
    int JOB_TRIGGER_TYPE_3 = 3;

    /**
     * 错过触发策略，1：忽略，2：立即补偿触发一次
     */
    int JOB_TRIGGER_MISFIRE_STRATEGY_1 = 1;
    /**
     * 错过触发策略，1：忽略，2：立即补偿触发一次
     */
    int JOB_TRIGGER_MISFIRE_STRATEGY_2 = 2;

    /**
     * 是否允许多节点并发执行，0：禁止，1：允许
     */
    int JOB_ALLOW_CONCURRENT_0 = 0;
    /**
     * 是否允许多节点并发执行，0：禁止，1：允许
     */
    int JOB_ALLOW_CONCURRENT_1 = 1;

    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_0 = 0;
    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_1 = 1;
    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_2 = 2;
    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_3 = 3;

    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_1 = 1;
    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_2 = 2;
    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_3 = 3;
    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_4 = 4;

    /**
     * 是否禁用：0-启用，1-禁用
     */
    int JOB_DISABLE_0 = 0;
    /**
     * 是否禁用：0-启用，1-禁用
     */
    int JOB_DISABLE_1 = 1;

    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_1 = 1;
    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_2 = 2;
    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_3 = 3;
    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_4 = 4;
}
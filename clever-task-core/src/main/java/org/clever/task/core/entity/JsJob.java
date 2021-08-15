package org.clever.task.core.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * js脚本任务(JsJob)实体类
 *
 * @author lizw
 * @since 2021-08-15 13:09:31
 */
@Data
public class JsJob implements Serializable {
    private static final long serialVersionUID = -24645952045245688L;
    /**
     * 主键id
     */
    private Long id;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * js文件id
     */
    private Long fileResourceId;

    /**
     * 创建时间
     */
    private Date createAt;

    /**
     * 更新时间
     */
    private Date updateAt;

}

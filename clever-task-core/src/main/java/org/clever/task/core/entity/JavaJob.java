package org.clever.task.core.entity;

import java.util.Date;
import java.io.Serializable;

import lombok.Data;

/**
 * js脚本任务(JavaJob)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:31
 */
@Data
public class JavaJob implements Serializable {
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
     * java class全路径
     */
    private String className;

    /**
     * java class method
     */
    private String classMethod;

    /**
     * 创建时间
     */
    private Date createAt;

    /**
     * 更新时间
     */
    private Date updateAt;
}

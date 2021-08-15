package org.clever.task.core.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * js脚本任务(ShellJob)实体类
 *
 * @author lizw
 * @since 2021-08-15 13:09:32
 */
@Data
public class ShellJob implements Serializable {
    private static final long serialVersionUID = -10214801187263077L;
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
     * shell文件id
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

package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.entity.FileResource;
import org.clever.task.core.entity.ShellJob;
import org.springframework.util.Assert;

import java.util.UUID;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:07 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ShellJobModel extends AbstractJob {
    /**
     * shell文件内容
     */
    private String content;

    public ShellJobModel(String name, String content) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(content, "参数content不能为空");
        this.name = name;
        this.content = content;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_4;
    }

    public FileResource toFileResource() {
        FileResource fileResource = new FileResource();
        fileResource.setModule(EnumConstant.FILE_RESOURCE_MODULE_4);
        fileResource.setName(String.format("%s_%s.sh", getName(), UUID.randomUUID()));
        fileResource.setContent(getContent());
        fileResource.setIsFile(EnumConstant.FILE_RESOURCE_IS_FILE_1);
        fileResource.setReadOnly(EnumConstant.FILE_RESOURCE_READ_ONLY_0);
        fileResource.setDescription(getDescription());
        return fileResource;
    }

    public ShellJob toJobEntity() {
        return new ShellJob();
    }
}

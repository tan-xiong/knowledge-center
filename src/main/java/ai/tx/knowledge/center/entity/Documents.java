package ai.tx.knowledge.center.entity;

import ai.tx.knowledge.center.common.IdUtils;
import lombok.Data;

import java.io.Serializable;

/**
 * @author tanxiong
 * @date 2025/7/14 9:58
 */
@Data
public class Documents implements Serializable {

    private String id;

    private String documentName;

    private String category;

    /**
     * 生成唯一ID
     */
    public void genId() {
        this.id = IdUtils.generateId();
    }

}

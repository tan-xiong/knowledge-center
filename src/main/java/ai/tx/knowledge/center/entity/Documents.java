package ai.tx.knowledge.center.entity;


import cn.hutool.core.lang.UUID;
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

    public void genId(){
        // 后续改为雪花算法
        this.id= UUID.randomUUID().toString(true);
    }


}

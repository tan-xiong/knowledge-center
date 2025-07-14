package ai.tx.knowledge.center.entity;


import cn.hutool.core.lang.UUID;
import lombok.Data;

/**
 * @author tanxiong
 * @date 2025/7/14 9:58
 */
@Data
public class Documents {

    private String id;

    private String documentName;

    private String category;

    public void genId(){
        this.id= UUID.randomUUID().toString(true);
    }


}

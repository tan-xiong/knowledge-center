package ai.tx.knowledge.center.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author tanxiong
 * @date 2025/7/14 9:58
 */
@Data
@Entity
@Table(name = "documents")
public class DocumentsDO {

    @Id
    private String id;

    private String documentName;

    private String category;


}

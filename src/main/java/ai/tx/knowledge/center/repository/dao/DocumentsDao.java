package ai.tx.knowledge.center.repository.dao;

import ai.tx.knowledge.center.model.DocumentsDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author tanxiong
 * @date 2025/7/14 10:09
 */
@Repository
public interface DocumentsDao extends JpaRepository<DocumentsDO, String> {

    List<DocumentsDO> findByCategory(String category);
}

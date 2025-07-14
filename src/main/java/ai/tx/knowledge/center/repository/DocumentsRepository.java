package ai.tx.knowledge.center.repository;

import ai.tx.knowledge.center.entity.Documents;
import ai.tx.knowledge.center.model.DocumentsDO;
import ai.tx.knowledge.center.repository.dao.DocumentsDao;
import cn.hutool.core.bean.BeanUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author tanxiong
 * @date 2025/7/14 10:11
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentsRepository {

    DocumentsDao documentsDao;

    public void save(Documents documents){
        DocumentsDO documentsDO = BeanUtil.copyProperties(documents, DocumentsDO.class);
        documentsDao.save(documentsDO);
    }

    public void delById(String id){
        documentsDao.deleteById(id);
    }

    public List<Documents> findByCategory(String category){
        List<DocumentsDO> documentsDOS = documentsDao.findByCategory(category);
        return BeanUtil.copyToList(documentsDOS,Documents.class);
    }


}

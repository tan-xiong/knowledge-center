package ai.tx.knowledge.center.service;

import ai.tx.knowledge.center.entity.Documents;
import ai.tx.knowledge.center.repository.DocumentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**j
 * 文档处理服务
 */
@Slf4j
@Service
public class KnowledgeFileService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentsRepository documentsRepository;

    /**
     * 支持的文件类型
     */
    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "pdf", "application/pdf",
            "txt", "text/plain"
    );

    private static final int batchSize = 25;


    /**
     * 处理上传的文档
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Document> processDocument(MultipartFile file, Documents document) {
        try {
            // 1. 文件验证
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                return null;
            }

            String fileType = getFileExtension(fileName).toLowerCase();
            if (!SUPPORTED_TYPES.containsKey(fileType)) {
                throw new Exception("不支持的文件类型: " + fileType + "，支持的类型：" + SUPPORTED_TYPES.keySet());
            }

            // 2. 文件大小检查
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB限制
                throw new Exception("文件大小不能超过10MB");
            }

            log.info("开始处理文档: {}, 类型: {}, 大小: {} bytes", fileName, fileType, file.getSize());

            // 3. 根据文件类型处理
            List<Document> documents = processDocumentByType(file, fileType);

            if (documents == null || documents.isEmpty()) {
                throw new Exception("文档处理后无有效内容");
            }

            // 4. 为文档添加元数据
            documents.forEach(doc -> {
                doc.getMetadata().put("documentId", document.getId());
                doc.getMetadata().put("source", fileName);
                doc.getMetadata().put("fileType", fileType);
                doc.getMetadata().put("fileSize", String.valueOf(file.getSize()));
                doc.getMetadata().put("uploadTime", String.valueOf(System.currentTimeMillis()));
            });

            document.setDocumentName(fileName);

            log.info("文档处理完成: {}, 生成片段数: {}", fileName, documents.size());

            // 3. 切分文档（如果单个文档太大）
            List<Document> splitDocuments = splitDocuments(documents);

            log.info("文档切分完成，共 {} 个文档片段", splitDocuments.size());

            // 4. 存储到向量数据库
            IntStream.range(0, (splitDocuments.size() + batchSize - 1) / batchSize)
//                    .parallel() //使用并行，会超过服务端允许每秒请求数。
                    .forEach(i -> {
                        int start = i * batchSize;
                        int end = Math.min(start + batchSize, splitDocuments.size());
                        List<Document> batch = splitDocuments.subList(start, end);
                        vectorStore.add(batch);
                        log.info("线程 {} 已存储第 {} - {} 个文档片段", Thread.currentThread().getName(), start + 1, end);
                    });

            log.info("知识文件上传成功: {}, 共处理 {} 个文档片段",
                    file.getOriginalFilename(), splitDocuments.size());

            documentsRepository.save(document);

            return documents;

        } catch (Exception e) {
            log.error("文档处理失败", e);
            return null;
        }
    }

    public List<Map<String, Object>> search(String query){
        // 执行搜索
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .similarityThreshold(0.50)
                        .topK(10)
                        .build()
        );

        log.info("搜索完成，找到 [{}] 个相关结果", results.size());

        // 格式化结果
        List<Map<String, Object>> formattedResults = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("index", i + 1);
            item.put("content", doc.getText());
            item.put("metadata", doc.getMetadata());
            formattedResults.add(item);
        }

        return formattedResults;

    }


    /**
     * 获取支持的文件类型
     */
    public Map<String, String> getSupportedTypes() {
        return new HashMap<>(SUPPORTED_TYPES);
    }


    @Transactional(rollbackFor = Exception.class)
    public void del(String id){
        documentsRepository.delById(id);
        FilterExpressionBuilder filterBuilder =  new FilterExpressionBuilder();
        Filter.Expression documentId = filterBuilder.eq("documentId", id).build();
        vectorStore.delete(documentId);
    }

    /**
     * 根据文件类型处理文档
     */
    private List<Document> processDocumentByType(MultipartFile file, String fileType) throws IOException {
        switch (fileType) {
            case "pdf":
                return processPdfDocument(file);
            case "txt":
                return processTextDocument(file);
            default:
                return null;
        }
    }

    /**
     * 处理PDF文档
     */
    private List<Document> processPdfDocument(MultipartFile file) throws IOException {
        Resource resource = new InputStreamResource(file.getInputStream());
        List<Document> documents = new TikaDocumentReader(resource)
                .get();
        log.info("PDF文档处理完成，共 {} 个文档", documents.size());
        return documents;
    }

    /**
     * 处理文本文档
     */
    private List<Document> processTextDocument(MultipartFile file) throws IOException {
        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        DocumentReader reader = new TextReader(resource);
        List<Document> documents = reader.get();

        log.info("文本文档处理完成，共 {} 个文档", documents.size());
        return documents;
    }


    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }


    /**
     * 切分文档
     */
    private List<Document> splitDocuments(List<Document> documents) {
        List<Document> splitDocuments = new ArrayList<>();

        for (Document document : documents) {
            String content = document.getText();
            if (content != null && !content.trim().isEmpty()) {
                // 如果文档太长，按段落分割
                if (content.length() > 1000) {
                    String[] parts = content.split("\\n\\s*\\n");
                    for (String part : parts) {
                        String trimmedPart = part.trim();
                        if (!trimmedPart.isEmpty()) {
                            Document doc = new Document(trimmedPart);
                            // 复制原始元数据
                            doc.getMetadata().putAll(document.getMetadata());
                            doc.getMetadata().put("chunk_id", String.valueOf(splitDocuments.size()));
                            splitDocuments.add(doc);
                        }
                    }
                } else {
                    // 文档不长，直接添加
                    Document doc = new Document(content);
                    doc.getMetadata().putAll(document.getMetadata());
                    doc.getMetadata().put("chunk_id", String.valueOf(splitDocuments.size()));
                    splitDocuments.add(doc);
                }
            }
        }

        return splitDocuments;
    }


} 
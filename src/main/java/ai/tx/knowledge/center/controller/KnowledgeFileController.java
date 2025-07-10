package ai.tx.knowledge.center.controller;

import ai.tx.knowledge.center.common.Result;
import ai.tx.knowledge.center.common.ResultCode;
import ai.tx.knowledge.center.service.DocumentProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 知识文件控制器
 */
@RequestMapping("/knowledge")
@RestController
public class KnowledgeFileController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFileController.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentProcessService documentProcessService;

    private static final int batchSize = 25;

    /**
     * 上传知识文件（支持多种格式）
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadKnowledgeFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 参数验证
            if (file == null || file.isEmpty()) {
                return Result.error(ResultCode.BAD_REQUEST.getCode(), "请选择要上传的文件");
            }

            log.info("开始上传知识文件: {}", file.getOriginalFilename());

            // 2. 处理文档
            List<Document> documents = documentProcessService.processDocument(file);
            if (documents == null || documents.isEmpty()) {
                return Result.error(ResultCode.KNOWLEDGE_UPLOAD_ERROR.getCode(), "文档处理后无有效内容");
            }

            // 3. 切分文档（如果单个文档太大）
            List<Document> splitDocuments = splitDocuments(documents);
            
            log.info("文档切分完成，共 {} 个文档片段", splitDocuments.size());

            // 4. 存储到向量数据库
            IntStream.range(0, (splitDocuments.size() + batchSize - 1) / batchSize)
                    .parallel()
                    .forEach(i -> {
                        int start = i * batchSize;
                        int end = Math.min(start + batchSize, splitDocuments.size());
                        List<Document> batch = splitDocuments.subList(start, end);
                        vectorStore.add(batch);
                        log.info("线程 {} 已存储第 {} - {} 个文档片段", Thread.currentThread().getName(), start + 1, end);
                    });
            
            log.info("知识文件上传成功: {}, 共处理 {} 个文档片段", 
                file.getOriginalFilename(), splitDocuments.size());

            // 5. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("originalDocuments", documents.size());
            result.put("splitDocuments", splitDocuments.size());
            result.put("fileType", getFileExtension(file.getOriginalFilename()));

            return Result.success("知识文件上传成功", result);

        } catch (Exception e) {
            log.error("上传知识文件失败", e);
            return Result.error(ResultCode.KNOWLEDGE_UPLOAD_ERROR.getCode(), 
                "上传知识文件失败: " + e.getMessage());
        }
    }


    /**
     * 搜索知识库
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam("query") String query) {
        try {
            // 参数验证
            if (!StringUtils.hasText(query)) {
                return Result.error(ResultCode.BAD_REQUEST.getCode(), "查询内容不能为空");
            }

            log.info("开始搜索知识库 - query: {}", query);

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

            return Result.success("搜索成功", formattedResults);

        } catch (Exception e) {
            log.error("搜索知识库失败", e);
            return Result.error(ResultCode.KNOWLEDGE_SEARCH_ERROR.getCode(), 
                "搜索知识库失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的文件类型
     */
    @GetMapping("/supportedTypes")
    public Result<Map<String, String>> getSupportedTypes() {
        try {
            Map<String, String> supportedTypes = documentProcessService.getSupportedTypes();
            return Result.success("获取支持的文件类型成功", supportedTypes);
        } catch (Exception e) {
            log.error("获取支持的文件类型失败", e);
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), 
                "获取支持的文件类型失败: " + e.getMessage());
        }
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

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
}

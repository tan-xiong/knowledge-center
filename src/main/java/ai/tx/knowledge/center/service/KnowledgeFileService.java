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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;

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

    @Value("${rag.chunking.target-size:800}")
    private int targetChunkSize;

    @Value("${rag.chunking.max-size:1200}")
    private int maxChunkSize;

    @Value("${rag.chunking.min-size:200}")
    private int minChunkSize;

    @Value("${rag.chunking.overlap-size:150}")
    private int overlapSize;

    @Value("${rag.chunking.sentence-boundary-mode:strict}")
    private String sentenceBoundaryMode;

    @Value("${rag.chunking.enable-content-hash:true}")
    private boolean enableContentHash;

    @Value("${rag.chunking.preserve-structure:true}")
    private boolean preserveStructure;

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
     * 混合策略：递归切分 + 语义边界 + 重叠窗口
     */
    private List<Document> splitDocuments(List<Document> documents) {
        List<Document> resultChunks = new ArrayList<>();
        
        for (Document originalDoc : documents) {
            String content = originalDoc.getText();
            if (content == null || content.trim().isEmpty()) {
                continue;
            }
            
            // 1. 文本预处理和标准化
            String normalizedContent = normalizeText(content);
            if (normalizedContent.length() < minChunkSize) {
                // 短文档直接作为一个chunk
                Document chunk = createChunkDocument(normalizedContent, originalDoc.getMetadata(), 0, 0, normalizedContent.length());
                resultChunks.add(chunk);
                continue;
            }
            
            // 2. 句子级别切分
            List<SentenceInfo> sentences = splitIntoSentences(normalizedContent);
            if (sentences.isEmpty()) {
                continue;
            }
            
            // 3. 智能分块：基于目标大小和语义边界
            List<ChunkInfo> chunks = createSmartChunks(sentences, normalizedContent);
            
            // 4. 转换为Document对象并添加元数据
            for (int i = 0; i < chunks.size(); i++) {
                ChunkInfo chunkInfo = chunks.get(i);
                String chunkText = normalizedContent.substring(chunkInfo.startOffset, chunkInfo.endOffset);
                Document chunkDoc = createChunkDocument(chunkText, originalDoc.getMetadata(), i, chunkInfo.startOffset, chunkInfo.endOffset);
                resultChunks.add(chunkDoc);
            }
        }
        
        log.info("文档切分完成，共生成 {} 个智能chunk", resultChunks.size());
        return resultChunks;
    }

    
    /**
     * 文本标准化处理
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        
        // 1. 统一换行符
        String normalized = text.replaceAll("\\r\\n?", "\n");
        
        // 2. 清理多余空白字符
        normalized = normalized.replaceAll("[\\t\\u000B\\f\\u00A0]+", " ");
        
        // 3. 压缩多个连续空格为单个空格
        normalized = normalized.replaceAll(" +", " ");
        
        // 4. 清理多个连续换行符
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        
        return normalized.trim();
    }
    
    /**
     * 句子切分 - 支持中英文标点符号
     */
    private List<SentenceInfo> splitIntoSentences(String text) {
        List<SentenceInfo> sentences = new ArrayList<>();
        
        // 句子边界模式：strict(严格) 或 relaxed(宽松)
        String patternStr = "strict".equals(sentenceBoundaryMode) 
            ? "(?<=[。！？：；.!?;])\\s+|(?<=\\.\\s)(?=[A-Z])|\\n{2,}"  // 严格模式：标点+空白 或 段落分隔
            : "(?<=[。！？.!?])\\s*|\\n{2,}";  // 宽松模式：仅主要标点
            
        Pattern sentencePattern = Pattern.compile(patternStr);
        Matcher matcher = sentencePattern.matcher(text);
        
        int lastEnd = 0;
        while (matcher.find()) {
            int start = lastEnd;
            int end = matcher.start();
            
            if (end > start) {
                String sentence = text.substring(start, end).trim();
                if (!sentence.isEmpty()) {
                    // 重新计算trim后的精确偏移量
                    int actualStart = findActualStart(text, start, end);
                    int actualEnd = findActualEnd(text, actualStart, end);
                    sentences.add(new SentenceInfo(actualStart, actualEnd));
                }
            }
            lastEnd = matcher.end();
        }
        
        // 处理最后一个句子
        if (lastEnd < text.length()) {
            String sentence = text.substring(lastEnd).trim();
            if (!sentence.isEmpty()) {
                int actualStart = findActualStart(text, lastEnd, text.length());
                int actualEnd = findActualEnd(text, actualStart, text.length());
                sentences.add(new SentenceInfo(actualStart, actualEnd));
            }
        }
        
        // 如果没有检测到句子边界，将整个文本作为一个句子
        if (sentences.isEmpty() && !text.trim().isEmpty()) {
            sentences.add(new SentenceInfo(0, text.length()));
        }
        
        return sentences;
    }
    
    /**
     * 智能分块算法 - 贪心聚合 + 重叠窗口
     */
    private List<ChunkInfo> createSmartChunks(List<SentenceInfo> sentences, String text) {
        List<ChunkInfo> chunks = new ArrayList<>();
        if (sentences.isEmpty()) return chunks;
        
        int currentIndex = 0;
        while (currentIndex < sentences.size()) {
            int chunkStart = sentences.get(currentIndex).startOffset;
            int chunkEnd = chunkStart;
            int sentenceCount = 0;
            
            // 贪心聚合：在目标大小内尽可能多地包含句子
            for (int i = currentIndex; i < sentences.size(); i++) {
                SentenceInfo sentence = sentences.get(i);
                int candidateEnd = sentence.endOffset;
                int candidateSize = candidateEnd - chunkStart;
                
                // 检查是否超过最大限制
                if (candidateSize > maxChunkSize) {
                    if (sentenceCount == 0) {
                        // 单个句子过长，强制切分
                        chunkEnd = Math.min(chunkStart + maxChunkSize, text.length());
                        break;
                    } else {
                        // 已有句子，停止聚合
                        break;
                    }
                }
                
                chunkEnd = candidateEnd;
                sentenceCount++;
                
                // 达到目标大小，可以停止聚合
                if (candidateSize >= targetChunkSize) {
                    break;
                }
            }
            
            // 确保chunk至少有最小大小（除非是最后一个chunk）
            if (chunkEnd - chunkStart < minChunkSize && currentIndex + sentenceCount < sentences.size()) {
                // 尝试添加更多句子达到最小大小
                for (int i = currentIndex + sentenceCount; i < sentences.size(); i++) {
                    SentenceInfo sentence = sentences.get(i);
                    int candidateEnd = sentence.endOffset;
                    if (candidateEnd - chunkStart <= maxChunkSize) {
                        chunkEnd = candidateEnd;
                        sentenceCount++;
                        if (chunkEnd - chunkStart >= minChunkSize) break;
                    } else {
                        break;
                    }
                }
            }
            
            chunks.add(new ChunkInfo(chunkStart, chunkEnd));
            
            // 计算下一个chunk的起始位置（考虑重叠）
            int nextStart = Math.max(chunkEnd - overlapSize, chunkStart + 1);
            currentIndex = findNextSentenceIndex(sentences, nextStart, currentIndex + 1);
            
            // 避免无限循环
            if (currentIndex <= 0) {
                currentIndex = Math.max(currentIndex + sentenceCount, currentIndex + 1);
            }
        }
        
        return chunks;
    }
    
    /**
     * 创建chunk文档对象
     */
    private Document createChunkDocument(String chunkText, Map<String, Object> originalMetadata, 
                                       int chunkIndex, int startOffset, int endOffset) {
        Map<String, Object> metadata = new HashMap<>(originalMetadata);
        
        // 基础元数据
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("startOffset", startOffset);
        metadata.put("endOffset", endOffset);
        metadata.put("chunkSize", chunkText.length());
        metadata.put("createdAt", System.currentTimeMillis());
        metadata.put("chunkingMethod", "enterprise_smart_chunking_v1.0");
        
        // 稳定的chunk ID
        if (enableContentHash) {
            String documentId = String.valueOf(originalMetadata.getOrDefault("documentId", ""));
            String stableId = generateStableChunkId(documentId, startOffset, endOffset, chunkText);
            metadata.put("chunkId", stableId);
            metadata.put("contentHash", hashString(chunkText));
        } else {
            metadata.put("chunkId", "chunk_" + chunkIndex);
        }
        
        // 结构信息（如果启用）
        if (preserveStructure) {
            analyzeAndAddStructureInfo(chunkText, metadata);
        }
        
        return new Document(chunkText, metadata);
    }
    
    /**
     * 生成稳定的chunk ID
     */
    private String generateStableChunkId(String documentId, int startOffset, int endOffset, String content) {
        String input = documentId + ":" + startOffset + ":" + endOffset + ":" + hashString(content);
        return hashString(input).substring(0, 16); // 取前16位作为ID
    }
    
    /**
     * 分析并添加结构信息
     */
    private void analyzeAndAddStructureInfo(String chunkText, Map<String, Object> metadata) {
        // 检测标题
        if (chunkText.matches("^#+\\s+.+") || chunkText.matches("^[A-Z\\s]+:.*")) {
            metadata.put("structureType", "heading");
        }
        // 检测列表
        else if (chunkText.matches("^\\s*[\\-\\*\\+]\\s+.*") || chunkText.matches("^\\s*\\d+\\.\\s+.*")) {
            metadata.put("structureType", "list");
        }
        // 检测表格迹象
        else if (chunkText.contains("|") && chunkText.lines().count() > 1) {
            metadata.put("structureType", "table");
        }
        // 检测代码块
        else if (chunkText.contains("```") || chunkText.matches(".*\\{.*\\}.*")) {
            metadata.put("structureType", "code");
        }
        // 普通段落
        else {
            metadata.put("structureType", "paragraph");
        }
        
        // 计算语义密度（非停用词比例）
        String[] words = chunkText.toLowerCase().split("\\s+");
        List<String> stopWords = List.of("的", "是", "在", "有", "和", "了", "不", "一", "这", "那", "我", "你", "他", 
                                       "the", "and", "is", "in", "to", "of", "a", "that", "it", "with", "for");
        long contentWords = java.util.Arrays.stream(words)
                .filter(word -> !stopWords.contains(word) && word.length() > 1)
                .count();
        double semanticDensity = words.length > 0 ? (double) contentWords / words.length : 0.0;
        metadata.put("semanticDensity", Math.round(semanticDensity * 100.0) / 100.0);
    }
    
    // ------------------------- 辅助工具方法 -------------------------
    
    private int findActualStart(String text, int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start;
    }
    
    private int findActualEnd(String text, int start, int end) {
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return end;
    }
    
    private int findNextSentenceIndex(List<SentenceInfo> sentences, int targetOffset, int minIndex) {
        for (int i = minIndex; i < sentences.size(); i++) {
            if (sentences.get(i).startOffset >= targetOffset) {
                return i;
            }
        }
        return sentences.size();
    }
    
    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
    
    // ------------------------- 内部数据结构 -------------------------
    
    private static class SentenceInfo {
        final int startOffset;
        final int endOffset;
        
        SentenceInfo(int startOffset, int endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
    
    private static class ChunkInfo {
        final int startOffset;
        final int endOffset;
        
        ChunkInfo(int startOffset, int endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }


} 
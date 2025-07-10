package ai.tx.knowledge.center.service;

import ai.tx.knowledge.center.common.Result;
import ai.tx.knowledge.center.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档处理服务
 */
@Slf4j
@Service
public class DocumentProcessService {

    /**
     * 支持的文件类型
     */
    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "pdf", "application/pdf",
            "txt", "text/plain"
    );

    /**
     * 处理上传的文档
     */
    public List<Document> processDocument(MultipartFile file) {
        try {
            // 1. 文件验证
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                return null;
            }

            String fileType = getFileExtension(fileName).toLowerCase();
            if (!SUPPORTED_TYPES.containsKey(fileType)) {
                throw new Exception( "不支持的文件类型: " + fileType + "，支持的类型：" + SUPPORTED_TYPES.keySet());
            }

            // 2. 文件大小检查
            if (file.getSize() > 10 * 1024 * 1024) { // 10MB限制
                throw new Exception("文件大小不能超过10MB");
            }

            log.info("开始处理文档: {}, 类型: {}, 大小: {} bytes", fileName, fileType, file.getSize());

            // 3. 根据文件类型处理
            List<Document> documents = processDocumentByType(file, fileType);

            // 4. 为文档添加元数据
            documents.forEach(doc -> {
                doc.getMetadata().put("source", fileName);
                doc.getMetadata().put("fileType", fileType);
                doc.getMetadata().put("fileSize", String.valueOf(file.getSize()));
                doc.getMetadata().put("uploadTime", String.valueOf(System.currentTimeMillis()));
            });

            log.info("文档处理完成: {}, 生成片段数: {}", fileName, documents.size());
            return documents;

        } catch (Exception e) {
            log.error("文档处理失败", e);
            return null;
        }
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
     * 获取支持的文件类型
     */
    public Map<String, String> getSupportedTypes() {
        return new HashMap<>(SUPPORTED_TYPES);
    }
} 
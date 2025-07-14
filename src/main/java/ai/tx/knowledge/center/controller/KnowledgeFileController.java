package ai.tx.knowledge.center.controller;

import ai.tx.knowledge.center.common.Result;
import ai.tx.knowledge.center.common.ResultCode;
import ai.tx.knowledge.center.dto.DocumentsDTO;
import ai.tx.knowledge.center.entity.Documents;
import ai.tx.knowledge.center.service.KnowledgeFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识文件控制器
 */
@RequestMapping("/knowledge")
@RestController
public class KnowledgeFileController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeFileController.class);


    @Autowired
    private KnowledgeFileService knowledgeFileService;


    /**
     * 上传知识文件（支持多种格式）
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadKnowledgeFile(@RequestParam("file") MultipartFile file, @RequestParam("category") String category) {
        try {
            // 1. 参数验证
            if (file == null || file.isEmpty()) {
                return Result.error(ResultCode.BAD_REQUEST.getCode(), "请选择要上传的文件");
            }

            log.info("开始上传知识文件: {}", file.getOriginalFilename());
            Documents document = new Documents();
            document.genId();
            document.setCategory(category);

            // 2. 处理文档
            knowledgeFileService.processDocument(file, document);

            // 3. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
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
            List<Map<String, Object>> formattedResults = knowledgeFileService.search(query);

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
            Map<String, String> supportedTypes = knowledgeFileService.getSupportedTypes();
            return Result.success("获取支持的文件类型成功", supportedTypes);
        } catch (Exception e) {
            log.error("获取支持的文件类型失败", e);
            return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(),
                    "获取支持的文件类型失败: " + e.getMessage());
        }
    }


    @RequestMapping("/del")
    public Result<Void> del(@RequestBody DocumentsDTO dto) {
        knowledgeFileService.del(dto.getId());
        return Result.success();
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

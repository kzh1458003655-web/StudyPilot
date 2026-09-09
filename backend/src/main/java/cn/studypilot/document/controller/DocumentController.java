package cn.studypilot.document.controller;

import cn.studypilot.common.response.ApiResponse;
import cn.studypilot.common.config.RequestIdFilter;
import cn.studypilot.document.dto.DocumentImportResponse;
import cn.studypilot.document.dto.DocumentListResponse;
import cn.studypilot.document.service.DocumentImportService;
import cn.studypilot.document.service.DocumentResourceService;
import cn.studypilot.document.service.LocalDocumentStorage;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.http.HttpStatus;
import cn.studypilot.common.database.ConditionalOnStudyPilotDatabase;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@ConditionalOnStudyPilotDatabase
public class DocumentController {
  private static final Set<String> DOCUMENT_TYPES = Set.of("TEXTBOOK", "LECTURE", "KNOWLEDGE", "PAST_EXAM", "REFERENCE_ANSWER");
  private final LocalDocumentStorage storage; private final DocumentImportService importer; private final DocumentResourceService resources;
  public DocumentController(LocalDocumentStorage storage, DocumentImportService importer, DocumentResourceService resources) { this.storage = storage; this.importer = importer; this.resources = resources; }
  @GetMapping
  public ApiResponse<java.util.List<DocumentListResponse>> list(@RequestParam long projectId, HttpServletRequest request) {
    if (projectId <= 0) throw new IllegalArgumentException("课程项目编号必须为正数");
    var data = resources.list(projectId).stream().map(item -> new DocumentListResponse(item.id(), item.displayName(), item.documentType(), item.status(), item.pageCount(), item.chunkCount())).toList();
    return new ApiResponse<>(data, request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
  }
  @PostMapping(consumes = "multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<DocumentImportResponse> upload(@RequestParam long projectId, @RequestParam(required = false) String documentType,
      @RequestPart MultipartFile file, HttpServletRequest request) throws IOException {
    if (projectId <= 0) throw new IllegalArgumentException("课程项目编号必须为正数");
    if (file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空");
    if (documentType != null && !DOCUMENT_TYPES.contains(documentType)) throw new IllegalArgumentException("资料类型不合法");
    documentType = documentType == null ? classify(file.getOriginalFilename()) : documentType;
    var stored = storage.store(projectId, file);
    try (var source = Files.newInputStream(Path.of(stored.path()))) {
      var imported = importer.importPdf(projectId, stored.displayName(), documentType, stored.path(), stored.contentHash(), source);
      return new ApiResponse<>(new DocumentImportResponse(imported.documentId(), imported.indexId(), imported.pageCount(), imported.chunkCount()), request.getAttribute(RequestIdFilter.ATTRIBUTE).toString());
    }
  }
  /** Keeps the upload form simple while routing likely papers to the analysis workflow. */
  private String classify(String filename) {
    String value = filename == null ? "" : filename.toLowerCase(java.util.Locale.ROOT);
    if (value.contains("final") || value.contains("midterm") || value.contains("exam") || value.contains("试卷") || value.contains("真题")) return "PAST_EXAM";
    if (value.contains("answer") || value.contains("答案")) return "REFERENCE_ANSWER";
    return "LECTURE";
  }
  @DeleteMapping("/{documentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long documentId, @RequestParam long projectId) throws IOException {
    if (projectId <= 0 || documentId <= 0) throw new IllegalArgumentException("资料编号或课程项目编号不合法");
    resources.delete(projectId, documentId);
  }
}

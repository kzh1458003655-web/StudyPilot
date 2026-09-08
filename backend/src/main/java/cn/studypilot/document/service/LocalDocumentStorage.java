package cn.studypilot.document.service;

import cn.studypilot.document.config.LocalStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Local PDF storage only. Database stores the path and hash, never the PDF binary. */
@Service
public class LocalDocumentStorage {
  private final Path root;

  public LocalDocumentStorage(LocalStorageProperties properties) {
    if (properties.root() == null || properties.root().isBlank()) {
      throw new IllegalArgumentException("本地资料存储目录不能为空");
    }
    root = Path.of(properties.root()).toAbsolutePath().normalize();
  }

  public StoredFile store(long projectId, MultipartFile file) throws IOException {
    if (projectId <= 0) throw new IllegalArgumentException("课程项目编号必须为正数");
    String original = safeDisplayName(file.getOriginalFilename());
    if (!original.toLowerCase().endsWith(".pdf")) throw new IllegalArgumentException("只支持文字型 PDF 文件");
    Path directory = root.resolve(Long.toString(projectId)); Files.createDirectories(directory);
    Path target = directory.resolve(java.util.UUID.randomUUID() + ".pdf");
    try (InputStream input = file.getInputStream()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
    return new StoredFile(target.toString(), sha256(target), original);
  }

  private String safeDisplayName(String original) {
    if (original == null || original.isBlank()) return "document.pdf";
    String normalized = original.replace('\\', '/');
    String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
    return name.isBlank() ? "document.pdf" : name;
  }
  private String sha256(Path path) throws IOException {
    try (InputStream input = Files.newInputStream(path)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256"); input.transferTo(new java.io.OutputStream() {
        @Override public void write(int value) { digest.update((byte) value); }
        @Override public void write(byte[] value, int offset, int length) { digest.update(value, offset, length); }
      });
      return java.util.HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
  }
  /** Removes only a path created below the configured D-drive (or overridden) upload root. */
  public void delete(String savedPath) throws IOException {
    Path target = Path.of(savedPath).toAbsolutePath().normalize();
    if (!target.startsWith(root)) throw new IllegalArgumentException("资料文件路径不合法");
    Files.deleteIfExists(target);
  }
  public record StoredFile(String path, String contentHash, String displayName) {}
}

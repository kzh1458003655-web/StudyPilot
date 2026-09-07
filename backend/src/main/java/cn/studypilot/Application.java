package cn.studypilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.nio.file.Path;

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    System.setProperty("pdfbox.fontcache", Path.of("data", "pdfbox-font-cache").toAbsolutePath().toString());
    SpringApplication.run(Application.class, args);
  }
}

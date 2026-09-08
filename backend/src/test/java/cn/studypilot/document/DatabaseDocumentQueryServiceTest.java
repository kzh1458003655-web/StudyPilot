package cn.studypilot.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.studypilot.document.repository.DocumentRepository;
import cn.studypilot.document.model.DocumentReference;
import cn.studypilot.document.service.DatabaseDocumentQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DatabaseDocumentQueryServiceTest {
  @Test void exposesOnlyGroundedQaDocumentTypesForTheRequestedProject() {
    DocumentRepository documents = Mockito.mock(DocumentRepository.class);
    when(documents.findReadyDocuments(4L, List.of("TEXTBOOK", "LECTURE", "KNOWLEDGE")))
        .thenReturn(List.of(new DocumentReference("doc-a", "资料 A")));

    assertThat(new DatabaseDocumentQueryService(documents).availableDocuments("4"))
        .extracting("indexId").containsExactly("doc-a");
    verify(documents).findReadyDocuments(4L, List.of("TEXTBOOK", "LECTURE", "KNOWLEDGE"));
  }

  @Test void rejectsAnInvalidProjectIdBeforeItCanReachTheRepository() {
    assertThatThrownBy(() -> new DatabaseDocumentQueryService(Mockito.mock(DocumentRepository.class)).availableDocuments("0"))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("课程项目编号不合法");
  }
}

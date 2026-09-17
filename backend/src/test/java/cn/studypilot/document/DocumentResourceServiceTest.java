package cn.studypilot.document;

import static org.mockito.Mockito.*;

import cn.studypilot.document.model.RemovableDocument;
import cn.studypilot.document.repository.DocumentRepository;
import cn.studypilot.document.service.DocumentResourceService;
import cn.studypilot.document.service.LocalDocumentStorage;
import cn.studypilot.retrieval.gateway.DocumentIndexGateway;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class DocumentResourceServiceTest {
  @Test void removesIndexMetadataAndFileUsingTheSameProjectBoundary() throws Exception {
    DocumentRepository documents = Mockito.mock(DocumentRepository.class);
    DocumentIndexGateway index = Mockito.mock(DocumentIndexGateway.class);
    LocalDocumentStorage storage = Mockito.mock(LocalDocumentStorage.class);
    when(documents.findRemovable(7L, 9L)).thenReturn(new RemovableDocument(9L, "doc-9", "data/uploads/7/doc-9.pdf"));
    new DocumentResourceService(documents, index, storage).delete(7L, 9L);
    InOrder order = inOrder(documents, index, storage);
    order.verify(documents).findRemovable(7L, 9L);
    order.verify(index).remove("doc-9");
    order.verify(documents).delete(7L, 9L);
    order.verify(storage).delete("data/uploads/7/doc-9.pdf");
  }
}

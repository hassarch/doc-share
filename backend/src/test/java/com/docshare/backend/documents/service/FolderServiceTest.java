package com.docshare.backend.documents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.docshare.backend.common.exception.NotFoundException;
import com.docshare.backend.common.exception.ValidationException;
import com.docshare.backend.documents.entity.Document;
import com.docshare.backend.documents.entity.Folder;
import com.docshare.backend.documents.repository.DocumentRepository;
import com.docshare.backend.documents.repository.FolderRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

  @Mock private FolderRepository folderRepository;
  @Mock private DocumentRepository documentRepository;

  private FolderService folderService;

  private final UUID ownerId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    folderService = new FolderService(folderRepository, documentRepository);
  }

  @Test
  void createWithoutParentSavesRootFolder() {
    when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

    Folder result = folderService.create(ownerId, "Documents", null);

    assertThat(result.getName()).isEqualTo("Documents");
    assertThat(result.getOwnerId()).isEqualTo(ownerId);
    assertThat(result.getParentFolder()).isNull();
  }

  @Test
  void createWithParentOwnedByRequesterSucceeds() {
    UUID parentId = UUID.randomUUID();
    Folder parent = new Folder(ownerId, null, "Parent");
    when(folderRepository.findById(parentId)).thenReturn(Optional.of(parent));
    when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

    Folder result = folderService.create(ownerId, "Child", parentId);

    assertThat(result.getParentFolder()).isSameAs(parent);
  }

  @Test
  void createWithParentOwnedBySomeoneElseThrowsNotFound() {
    UUID parentId = UUID.randomUUID();
    Folder parent = new Folder(otherUserId, null, "Not Yours");
    when(folderRepository.findById(parentId)).thenReturn(Optional.of(parent));

    assertThatThrownBy(() -> folderService.create(ownerId, "Child", parentId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void getOwnedFolderThrowsNotFoundWhenOwnerMismatch() {
    UUID folderId = UUID.randomUUID();
    Folder folder = new Folder(otherUserId, null, "Theirs");
    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

    assertThatThrownBy(() -> folderService.getOwnedFolder(folderId, ownerId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deleteThrowsValidationExceptionWhenFolderHasSubfolders() {
    UUID folderId = UUID.randomUUID();
    Folder folder = new Folder(ownerId, null, "Parent");
    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
    when(folderRepository.findByParentFolderId(folderId))
        .thenReturn(List.of(new Folder(ownerId, folder, "Child")));

    assertThatThrownBy(() -> folderService.delete(folderId, ownerId))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("not empty");
  }

  @Test
  void deleteThrowsValidationExceptionWhenFolderHasDocuments() {
    UUID folderId = UUID.randomUUID();
    Folder folder = new Folder(ownerId, null, "Parent");
    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
    when(folderRepository.findByParentFolderId(folderId)).thenReturn(List.of());
    when(documentRepository.findByFolderIdAndDeletedFalse(folderId))
        .thenReturn(List.of(new Document(ownerId, "f.txt", folderId, 10, "text/plain", "hash")));

    assertThatThrownBy(() -> folderService.delete(folderId, ownerId))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void deleteSucceedsWhenFolderIsEmpty() {
    UUID folderId = UUID.randomUUID();
    Folder folder = new Folder(ownerId, null, "Empty");
    when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
    when(folderRepository.findByParentFolderId(folderId)).thenReturn(List.of());
    when(documentRepository.findByFolderIdAndDeletedFalse(folderId)).thenReturn(List.of());

    folderService.delete(folderId, ownerId);

    org.mockito.Mockito.verify(folderRepository).delete(folder);
  }
}

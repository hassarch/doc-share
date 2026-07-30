package com.docshare.backend.documents.service;

import com.docshare.backend.common.exception.NotFoundException;
import com.docshare.backend.common.exception.ValidationException;
import com.docshare.backend.documents.entity.Folder;
import com.docshare.backend.documents.repository.DocumentRepository;
import com.docshare.backend.documents.repository.FolderRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FolderService {

  private final FolderRepository folderRepository;
  private final DocumentRepository documentRepository;

  public FolderService(FolderRepository folderRepository, DocumentRepository documentRepository) {
    this.folderRepository = folderRepository;
    this.documentRepository = documentRepository;
  }

  @Transactional
  public Folder create(UUID ownerId, String name, UUID parentFolderId) {
    Folder parent = parentFolderId != null ? getOwnedFolder(parentFolderId, ownerId) : null;
    return folderRepository.save(new Folder(ownerId, parent, name));
  }

  public Folder getOwnedFolder(UUID folderId, UUID requesterId) {
    Folder folder =
        folderRepository
            .findById(folderId)
            .orElseThrow(() -> new NotFoundException("Folder not found"));
    if (!folder.getOwnerId().equals(requesterId)) {
      // Deliberately NotFoundException, not ForbiddenException: revealing
      // "this folder exists but isn't yours" leaks its existence to
      // someone who shouldn't know that. Full RBAC (shared folders) is a
      // Sharing-phase concern this deliberately doesn't attempt yet.
      throw new NotFoundException("Folder not found");
    }
    return folder;
  }

  public List<Folder> list(UUID ownerId, UUID parentFolderId) {
    return parentFolderId == null
        ? folderRepository.findByOwnerId(ownerId)
        : folderRepository.findByParentFolderId(parentFolderId);
  }

  @Transactional
  public Folder rename(UUID folderId, UUID requesterId, String newName) {
    Folder folder = getOwnedFolder(folderId, requesterId);
    folder.rename(newName);
    return folderRepository.save(folder);
  }

  @Transactional
  public void delete(UUID folderId, UUID requesterId) {
    Folder folder = getOwnedFolder(folderId, requesterId);

    boolean hasSubfolders = !folderRepository.findByParentFolderId(folderId).isEmpty();
    boolean hasDocuments = !documentRepository.findByFolderIdAndDeletedFalse(folderId).isEmpty();
    if (hasSubfolders || hasDocuments) {
      throw new ValidationException(
          "Folder is not empty — move or delete its contents before deleting the folder");
    }

    folderRepository.delete(folder);
  }
}

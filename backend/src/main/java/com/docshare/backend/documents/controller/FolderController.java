package com.docshare.backend.documents.controller;

import com.docshare.backend.common.util.CurrentUser;
import com.docshare.backend.documents.dto.CreateFolderRequest;
import com.docshare.backend.documents.dto.FolderResponse;
import com.docshare.backend.documents.dto.RenameFolderRequest;
import com.docshare.backend.documents.entity.Folder;
import com.docshare.backend.documents.service.FolderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {

  private final FolderService folderService;

  public FolderController(FolderService folderService) {
    this.folderService = folderService;
  }

  @PostMapping
  public ResponseEntity<FolderResponse> create(@Valid @RequestBody CreateFolderRequest request) {
    Folder folder =
        folderService.create(CurrentUser.id(), request.name(), request.parentFolderId());
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(folder));
  }

  @GetMapping
  public ResponseEntity<List<FolderResponse>> list(
      @RequestParam(required = false) UUID parentFolderId) {
    List<FolderResponse> folders =
        folderService.list(CurrentUser.id(), parentFolderId).stream()
            .map(FolderController::toResponse)
            .toList();
    return ResponseEntity.ok(folders);
  }

  @GetMapping("/{id}")
  public ResponseEntity<FolderResponse> get(@PathVariable UUID id) {
    Folder folder = folderService.getOwnedFolder(id, CurrentUser.id());
    return ResponseEntity.ok(toResponse(folder));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<FolderResponse> rename(
      @PathVariable UUID id, @Valid @RequestBody RenameFolderRequest request) {
    Folder folder = folderService.rename(id, CurrentUser.id(), request.name());
    return ResponseEntity.ok(toResponse(folder));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    folderService.delete(id, CurrentUser.id());
    return ResponseEntity.noContent().build();
  }

  private static FolderResponse toResponse(Folder folder) {
    UUID parentId = folder.getParentFolder() != null ? folder.getParentFolder().getId() : null;
    return new FolderResponse(
        folder.getId(), folder.getName(), parentId, folder.getCreatedAt(), folder.getUpdatedAt());
  }
}

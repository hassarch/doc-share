"use client";

import { useState, useCallback } from "react";
import { FolderPlus, Upload as UploadIcon, CheckSquare } from "lucide-react";
import { DocumentResponse, FolderResponse, downloadDocument } from "@/lib/documents-api";
import {
  useUploadDocument,
  useRenameDocument,
  useDeleteDocument,
  useDeleteFolder,
} from "@/hooks/use-documents";
import { UploadDropzone } from "./UploadDropzone";
import { Button } from "../common/Button";
import { CreateFolderDialog } from "./CreateFolderDialog";
import { RenameFileDialog } from "./RenameFileDialog";
import { ShareModal } from "../sharing/ShareModal";
import { ApiError } from "@/lib/api";
import { ViewSwitcher, ViewMode } from "./ViewSwitcher";
import { FileGridView } from "./FileGridView";
import { FileListView } from "./FileListView";
import { BulkActionsToolbar } from "./BulkActionsToolbar";
import { EnhancedEmptyState } from "./EnhancedEmptyState";
import { cn } from "@/lib/utils";

interface FileBrowserProps {
  documents: DocumentResponse[];
  folders: FolderResponse[];
  currentFolderId: string | null;
}

export function FileBrowser({ documents, folders, currentFolderId }: FileBrowserProps) {
  const [viewMode, setViewMode] = useState<ViewMode>("grid");
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const [renameDoc, setRenameDoc] = useState<DocumentResponse | null>(null);
  const [shareDoc, setShareDoc] = useState<DocumentResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set());
  const [isDragOver, setIsDragOver] = useState(false);

  const uploadMutation = useUploadDocument();
  const renameMutation = useRenameDocument();
  const deleteDocMutation = useDeleteDocument();
  const deleteFolderMutation = useDeleteFolder();

  const isEmpty = documents.length === 0 && folders.length === 0;
  const selectionMode = selectedItems.size > 0;

  // Selection handlers
  const handleToggleSelect = useCallback((id: string, type: "file" | "folder") => {
    const key = `${type}-${id}`;
    setSelectedItems((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }, []);

  const handleClearSelection = useCallback(() => {
    setSelectedItems(new Set());
  }, []);

  const handleSelectAll = useCallback(() => {
    const allKeys = [
      ...folders.map((f) => `folder-${f.id}`),
      ...documents.map((d) => `file-${d.id}`),
    ];
    setSelectedItems(new Set(allKeys));
  }, [folders, documents]);

  // Drag & Drop handlers
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback(
    async (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDragOver(false);

      const files = Array.from(e.dataTransfer.files);
      if (files.length > 0) {
        setShowUpload(true);
        // Upload first file (can be extended to handle multiple)
        try {
          setError(null);
          await uploadMutation.mutateAsync({ file: files[0], folderId: currentFolderId });
          setShowUpload(false);
        } catch (err) {
          setError(err instanceof ApiError ? err.message : "Upload failed");
        }
      }
    },
    [currentFolderId, uploadMutation]
  );

  // File operations
  async function handleUpload(file: File) {
    try {
      setError(null);
      await uploadMutation.mutateAsync({ file, folderId: currentFolderId });
      setShowUpload(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed");
    }
  }

  async function handleDownload(doc: DocumentResponse) {
    try {
      setError(null);
      await downloadDocument(doc.id, doc.filename);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Download failed");
    }
  }

  async function handleRename(id: string, filename: string) {
    try {
      setError(null);
      await renameMutation.mutateAsync({ id, filename });
      setRenameDoc(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Rename failed");
    }
  }

  async function handleDeleteDoc(id: string) {
    try {
      setError(null);
      await deleteDocMutation.mutateAsync(id);
      // Remove from selection if selected
      setSelectedItems((prev) => {
        const next = new Set(prev);
        next.delete(`file-${id}`);
        return next;
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Delete failed");
    }
  }

  async function handleDeleteFolder(id: string) {
    try {
      setError(null);
      await deleteFolderMutation.mutateAsync(id);
      // Remove from selection if selected
      setSelectedItems((prev) => {
        const next = new Set(prev);
        next.delete(`folder-${id}`);
        return next;
      });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Delete failed");
    }
  }

  // Bulk operations
  async function handleBulkDelete() {
    const fileIds = Array.from(selectedItems)
      .filter((key) => key.startsWith("file-"))
      .map((key) => key.replace("file-", ""));
    const folderIds = Array.from(selectedItems)
      .filter((key) => key.startsWith("folder-"))
      .map((key) => key.replace("folder-", ""));

    try {
      setError(null);
      await Promise.all([
        ...fileIds.map((id) => deleteDocMutation.mutateAsync(id)),
        ...folderIds.map((id) => deleteFolderMutation.mutateAsync(id)),
      ]);
      handleClearSelection();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Bulk delete failed");
    }
  }

  return (
    <div
      className={cn("p-4 md:p-6 h-full", isDragOver && "bg-primary-50")}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Drag Overlay */}
      {isDragOver && (
        <div className="fixed inset-0 bg-primary-600/10 backdrop-blur-sm z-50 flex items-center justify-center pointer-events-none">
          <div className="bg-white rounded-2xl p-8 shadow-2xl border-4 border-dashed border-primary-500 max-w-md text-center">
            <UploadIcon className="h-16 w-16 text-primary-600 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-neutral-900 mb-2">Drop files to upload</h3>
            <p className="text-neutral-600">Release to start uploading your files</p>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-xl font-semibold text-neutral-900">Files & Folders</h2>
          <p className="text-sm text-neutral-600 mt-1">
            {documents.length + folders.length} {documents.length + folders.length === 1 ? "item" : "items"}
          </p>
        </div>
        
        <div className="flex flex-wrap items-center gap-3">
          {!isEmpty && (
            <>
              <ViewSwitcher currentView={viewMode} onViewChange={setViewMode} />
              
              {selectedItems.size === 0 && (
                <button
                  onClick={handleSelectAll}
                  className="flex items-center gap-2 px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <CheckSquare className="h-4 w-4" />
                  Select
                </button>
              )}
            </>
          )}
          
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setShowCreateFolder(true)}
          >
            <FolderPlus className="h-4 w-4" />
            New Folder
          </Button>
          <Button
            size="sm"
            onClick={() => setShowUpload(true)}
          >
            <UploadIcon className="h-4 w-4" />
            Upload
          </Button>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-4 p-4 rounded-xl bg-error-50 border border-error-200 text-sm text-error-700 flex items-start gap-3">
          <span className="flex-1">{error}</span>
          <button
            onClick={() => setError(null)}
            className="text-error-700 hover:text-error-900 font-medium"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Upload Dropzone */}
      {showUpload && (
        <div className="mb-6">
          <UploadDropzone
            onUpload={handleUpload}
            isUploading={uploadMutation.isPending}
          />
        </div>
      )}

      {/* Content */}
      {isEmpty && !showUpload ? (
        <EnhancedEmptyState
          onUploadClick={() => setShowUpload(true)}
          onCreateFolderClick={() => setShowCreateFolder(true)}
        />
      ) : (
        <div className="mb-20">
          {viewMode === "grid" && (
            <FileGridView
              documents={documents}
              folders={folders}
              selectedItems={selectedItems}
              onToggleSelect={handleToggleSelect}
              onDownload={handleDownload}
              onRename={setRenameDoc}
              onShare={setShareDoc}
              onDeleteDoc={handleDeleteDoc}
              onDeleteFolder={handleDeleteFolder}
              isDeleting={deleteDocMutation.isPending || deleteFolderMutation.isPending}
              selectionMode={selectionMode}
            />
          )}

          {viewMode === "list" && (
            <FileListView
              documents={documents}
              folders={folders}
              selectedItems={selectedItems}
              onToggleSelect={handleToggleSelect}
              onDownload={handleDownload}
              onRename={setRenameDoc}
              onShare={setShareDoc}
              onDeleteDoc={handleDeleteDoc}
              onDeleteFolder={handleDeleteFolder}
              isDeleting={deleteDocMutation.isPending || deleteFolderMutation.isPending}
              selectionMode={selectionMode}
            />
          )}

          {viewMode === "table" && (
            <FileListView
              documents={documents}
              folders={folders}
              selectedItems={selectedItems}
              onToggleSelect={handleToggleSelect}
              onDownload={handleDownload}
              onRename={setRenameDoc}
              onShare={setShareDoc}
              onDeleteDoc={handleDeleteDoc}
              onDeleteFolder={handleDeleteFolder}
              isDeleting={deleteDocMutation.isPending || deleteFolderMutation.isPending}
              selectionMode={selectionMode}
            />
          )}
        </div>
      )}

      {/* Bulk Actions Toolbar */}
      <BulkActionsToolbar
        selectedCount={selectedItems.size}
        onClearSelection={handleClearSelection}
        onBulkDelete={handleBulkDelete}
      />

      {/* Dialogs */}
      <CreateFolderDialog
        open={showCreateFolder}
        onOpenChange={setShowCreateFolder}
        parentFolderId={currentFolderId}
      />

      {renameDoc && (
        <RenameFileDialog
          open={true}
          onOpenChange={(open) => !open && setRenameDoc(null)}
          document={renameDoc}
          onRename={handleRename}
          isRenaming={renameMutation.isPending}
        />
      )}

      {shareDoc && (
        <ShareModal
          open={true}
          onOpenChange={(open) => !open && setShareDoc(null)}
          document={shareDoc}
        />
      )}
    </div>
  );
}

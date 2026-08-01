"use client";

import { useState } from "react";
import { FolderPlus, Upload as UploadIcon } from "lucide-react";
import { DocumentResponse, FolderResponse, downloadDocument } from "@/lib/documents-api";
import {
  useUploadDocument,
  useRenameDocument,
  useDeleteDocument,
  useDeleteFolder,
} from "@/hooks/use-documents";
import { FileRow } from "./FileRow";
import { FolderRow } from "./FolderRow";
import { UploadDropzone } from "./UploadDropzone";
import { EmptyState } from "../common/EmptyState";
import { Button } from "../common/Button";
import { CreateFolderDialog } from "./CreateFolderDialog";
import { RenameFileDialog } from "./RenameFileDialog";
import { ShareModal } from "../sharing/ShareModal";
import { ApiError } from "@/lib/api";

interface FileBrowserProps {
  documents: DocumentResponse[];
  folders: FolderResponse[];
  currentFolderId: string | null;
}

export function FileBrowser({ documents, folders, currentFolderId }: FileBrowserProps) {
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const [renameDoc, setRenameDoc] = useState<DocumentResponse | null>(null);
  const [shareDoc, setShareDoc] = useState<DocumentResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const uploadMutation = useUploadDocument();
  const renameMutation = useRenameDocument();
  const deleteDocMutation = useDeleteDocument();
  const deleteFolderMutation = useDeleteFolder();

  const isEmpty = documents.length === 0 && folders.length === 0;

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
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Delete failed");
    }
  }

  async function handleDeleteFolder(id: string) {
    try {
      setError(null);
      await deleteFolderMutation.mutateAsync(id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Delete failed");
    }
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-graphite">Files & Folders</h2>
        <div className="flex gap-3">
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

      {error && (
        <div className="mb-4 p-3 rounded-lg bg-brick/10 border border-brick/20 text-sm text-brick">
          {error}
        </div>
      )}

      {showUpload && (
        <div className="mb-6">
          <UploadDropzone
            onUpload={handleUpload}
            isUploading={uploadMutation.isPending}
          />
        </div>
      )}

      {isEmpty && !showUpload ? (
        <EmptyState
          icon={<FolderPlus className="h-12 w-12" />}
          title="No files yet"
          description="Upload your first file or create a folder to get started"
          action={
            <Button onClick={() => setShowUpload(true)}>
              <UploadIcon className="h-4 w-4" />
              Upload file
            </Button>
          }
        />
      ) : (
        <div className="bg-paper rounded-lg border border-hairline overflow-hidden">
          {folders.map((folder) => (
            <FolderRow
              key={folder.id}
              folder={folder}
              onDelete={() => handleDeleteFolder(folder.id)}
              isDeleting={deleteFolderMutation.isPending}
            />
          ))}
          {documents.map((doc) => (
            <FileRow
              key={doc.id}
              document={doc}
              onDownload={() => handleDownload(doc)}
              onRename={() => setRenameDoc(doc)}
              onShare={() => setShareDoc(doc)}
              onDelete={() => handleDeleteDoc(doc.id)}
              isDeleting={deleteDocMutation.isPending}
            />
          ))}
        </div>
      )}

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

"use client";

import { useState } from "react";
import { FileText, Folder, Download, Trash2, Edit2, Share2, MoreVertical, Check } from "lucide-react";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useRouter } from "next/navigation";
import { DocumentResponse, FolderResponse } from "@/lib/documents-api";
import { formatBytes, formatDate, cn } from "@/lib/utils";
import { ConfirmDeleteDialog } from "../common/ConfirmDeleteDialog";

interface FileGridViewProps {
  documents: DocumentResponse[];
  folders: FolderResponse[];
  selectedItems: Set<string>;
  onToggleSelect: (id: string, type: "file" | "folder") => void;
  onDownload: (doc: DocumentResponse) => void;
  onRename: (doc: DocumentResponse) => void;
  onShare: (doc: DocumentResponse) => void;
  onDeleteDoc: (id: string) => void;
  onDeleteFolder: (id: string) => void;
  isDeleting?: boolean;
  selectionMode?: boolean;
}

export function FileGridView({
  documents,
  folders,
  selectedItems,
  onToggleSelect,
  onDownload,
  onRename,
  onShare,
  onDeleteDoc,
  onDeleteFolder,
  isDeleting,
  selectionMode,
}: FileGridViewProps) {
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
      {folders.map((folder) => (
        <FolderCard
          key={folder.id}
          folder={folder}
          isSelected={selectedItems.has(`folder-${folder.id}`)}
          onToggleSelect={() => onToggleSelect(folder.id, "folder")}
          onDelete={onDeleteFolder}
          isDeleting={isDeleting}
          selectionMode={selectionMode}
        />
      ))}
      {documents.map((doc) => (
        <FileCard
          key={doc.id}
          document={doc}
          isSelected={selectedItems.has(`file-${doc.id}`)}
          onToggleSelect={() => onToggleSelect(doc.id, "file")}
          onDownload={onDownload}
          onRename={onRename}
          onShare={onShare}
          onDelete={onDeleteDoc}
          isDeleting={isDeleting}
          selectionMode={selectionMode}
        />
      ))}
    </div>
  );
}

function FolderCard({
  folder,
  isSelected,
  onToggleSelect,
  onDelete,
  isDeleting,
  selectionMode,
}: {
  folder: FolderResponse;
  isSelected: boolean;
  onToggleSelect: () => void;
  onDelete: (id: string) => void;
  isDeleting?: boolean;
  selectionMode?: boolean;
}) {
  const router = useRouter();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  return (
    <>
      <div
        className={cn(
          "group relative bg-white rounded-xl border-2 transition-all duration-200",
          "hover:shadow-md hover:border-primary-200",
          "focus-within:ring-2 focus-within:ring-primary-500 focus-within:ring-offset-2",
          isSelected
            ? "border-primary-500 bg-primary-50"
            : "border-neutral-200"
        )}
      >
        {/* Selection Checkbox */}
        {selectionMode && (
          <div className="absolute top-3 left-3 z-10">
            <button
              onClick={(e) => {
                e.stopPropagation();
                onToggleSelect();
              }}
              className={cn(
                "h-5 w-5 rounded border-2 flex items-center justify-center transition-all",
                isSelected
                  ? "bg-primary-600 border-primary-600"
                  : "bg-white border-neutral-300 hover:border-primary-500"
              )}
            >
              {isSelected && <Check className="h-3 w-3 text-white" />}
            </button>
          </div>
        )}

        {/* Actions Menu */}
        <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity z-10">
          <DropdownMenu.Root>
            <DropdownMenu.Trigger className="p-1.5 bg-white rounded-lg shadow-md hover:bg-neutral-50 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500">
              <MoreVertical className="h-4 w-4 text-neutral-700" />
            </DropdownMenu.Trigger>

            <DropdownMenu.Portal>
              <DropdownMenu.Content
                className="min-w-[160px] bg-white rounded-xl p-2 shadow-xl border border-neutral-200"
                sideOffset={5}
              >
                <DropdownMenu.Item
                  onClick={() => setShowDeleteDialog(true)}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-error-600 rounded-lg hover:bg-error-50 cursor-pointer outline-none"
                >
                  <Trash2 className="h-4 w-4" />
                  Delete
                </DropdownMenu.Item>
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>
        </div>

        {/* Folder Content */}
        <div
          className="p-6 cursor-pointer"
          onClick={() => router.push(`/documents/${folder.id}`)}
        >
          <div className="flex flex-col items-center text-center">
            <div className="h-16 w-16 rounded-xl bg-primary-100 flex items-center justify-center mb-3">
              <Folder className="h-8 w-8 text-primary-600" />
            </div>
            <h3 className="font-medium text-neutral-900 text-sm truncate w-full mb-1">
              {folder.name}
            </h3>
            <p className="text-xs text-neutral-500">
              {formatDate(folder.updatedAt)}
            </p>
          </div>
        </div>
      </div>

      <ConfirmDeleteDialog
        open={showDeleteDialog}
        onOpenChange={setShowDeleteDialog}
        title="Delete folder"
        description={`Are you sure you want to delete "${folder.name}"? All files and subfolders will be deleted.`}
        onConfirm={() => {
          onDelete(folder.id);
          setShowDeleteDialog(false);
        }}
        isDeleting={isDeleting}
      />
    </>
  );
}

function FileCard({
  document,
  isSelected,
  onToggleSelect,
  onDownload,
  onRename,
  onShare,
  onDelete,
  isDeleting,
  selectionMode,
}: {
  document: DocumentResponse;
  isSelected: boolean;
  onToggleSelect: () => void;
  onDownload: (doc: DocumentResponse) => void;
  onRename: (doc: DocumentResponse) => void;
  onShare: (doc: DocumentResponse) => void;
  onDelete: (id: string) => void;
  isDeleting?: boolean;
  selectionMode?: boolean;
}) {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  return (
    <>
      <div
        className={cn(
          "group relative bg-white rounded-xl border-2 transition-all duration-200",
          "hover:shadow-md hover:border-primary-200",
          "focus-within:ring-2 focus-within:ring-primary-500 focus-within:ring-offset-2",
          isSelected
            ? "border-primary-500 bg-primary-50"
            : "border-neutral-200"
        )}
      >
        {/* Selection Checkbox */}
        {selectionMode && (
          <div className="absolute top-3 left-3 z-10">
            <button
              onClick={(e) => {
                e.stopPropagation();
                onToggleSelect();
              }}
              className={cn(
                "h-5 w-5 rounded border-2 flex items-center justify-center transition-all",
                isSelected
                  ? "bg-primary-600 border-primary-600"
                  : "bg-white border-neutral-300 hover:border-primary-500"
              )}
            >
              {isSelected && <Check className="h-3 w-3 text-white" />}
            </button>
          </div>
        )}

        {/* Actions Menu */}
        <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity z-10">
          <DropdownMenu.Root>
            <DropdownMenu.Trigger className="p-1.5 bg-white rounded-lg shadow-md hover:bg-neutral-50 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500">
              <MoreVertical className="h-4 w-4 text-neutral-700" />
            </DropdownMenu.Trigger>

            <DropdownMenu.Portal>
              <DropdownMenu.Content
                className="min-w-[160px] bg-white rounded-xl p-2 shadow-xl border border-neutral-200"
                sideOffset={5}
              >
                <DropdownMenu.Item
                  onClick={() => onDownload(document)}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-neutral-700 rounded-lg hover:bg-neutral-100 cursor-pointer outline-none"
                >
                  <Download className="h-4 w-4" />
                  Download
                </DropdownMenu.Item>
                <DropdownMenu.Item
                  onClick={() => onRename(document)}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-neutral-700 rounded-lg hover:bg-neutral-100 cursor-pointer outline-none"
                >
                  <Edit2 className="h-4 w-4" />
                  Rename
                </DropdownMenu.Item>
                <DropdownMenu.Item
                  onClick={() => onShare(document)}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-neutral-700 rounded-lg hover:bg-neutral-100 cursor-pointer outline-none"
                >
                  <Share2 className="h-4 w-4" />
                  Share
                </DropdownMenu.Item>
                <div className="h-px bg-neutral-200 my-1"></div>
                <DropdownMenu.Item
                  onClick={() => setShowDeleteDialog(true)}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-error-600 rounded-lg hover:bg-error-50 cursor-pointer outline-none"
                >
                  <Trash2 className="h-4 w-4" />
                  Delete
                </DropdownMenu.Item>
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>
        </div>

        {/* File Content */}
        <div className="p-6">
          <div className="flex flex-col items-center text-center">
            <div className="h-16 w-16 rounded-xl bg-neutral-100 flex items-center justify-center mb-3">
              <FileText className="h-8 w-8 text-neutral-600" />
            </div>
            <h3 className="font-medium text-neutral-900 text-sm truncate w-full mb-1">
              {document.filename}
            </h3>
            <p className="text-xs text-neutral-500">
              {formatBytes(document.sizeBytes)}
            </p>
          </div>
        </div>
      </div>

      <ConfirmDeleteDialog
        open={showDeleteDialog}
        onOpenChange={setShowDeleteDialog}
        title="Delete file"
        description={`Are you sure you want to delete "${document.filename}"?`}
        onConfirm={() => {
          onDelete(document.id);
          setShowDeleteDialog(false);
        }}
        isDeleting={isDeleting}
      />
    </>
  );
}

"use client";

import { useState } from "react";
import { FileText, Folder, Download, Trash2, Edit2, Share2, MoreVertical, Check } from "lucide-react";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useRouter } from "next/navigation";
import { DocumentResponse, FolderResponse } from "@/lib/documents-api";
import { formatBytes, formatDate, cn } from "@/lib/utils";
import { ConfirmDeleteDialog } from "../common/ConfirmDeleteDialog";

interface FileListViewProps {
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

export function FileListView({
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
}: FileListViewProps) {
  return (
    <div className="bg-white rounded-xl border border-neutral-200 overflow-hidden divide-y divide-neutral-100">
      {folders.map((folder) => (
        <FolderListItem
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
        <FileListItem
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

function FolderListItem({
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
          "group flex items-center gap-4 px-4 py-3 hover:bg-neutral-50 transition-colors",
          isSelected && "bg-primary-50"
        )}
      >
        {/* Selection Checkbox */}
        {selectionMode && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onToggleSelect();
            }}
            className={cn(
              "h-5 w-5 rounded border-2 flex items-center justify-center transition-all flex-shrink-0",
              isSelected
                ? "bg-primary-600 border-primary-600"
                : "bg-white border-neutral-300 hover:border-primary-500"
            )}
          >
            {isSelected && <Check className="h-3 w-3 text-white" />}
          </button>
        )}

        {/* Folder Icon & Info */}
        <div
          className="flex items-center gap-3 flex-1 min-w-0 cursor-pointer"
          onClick={() => router.push(`/documents/${folder.id}`)}
        >
          <div className="h-10 w-10 rounded-lg bg-primary-100 flex items-center justify-center flex-shrink-0">
            <Folder className="h-5 w-5 text-primary-600" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-neutral-900 truncate">{folder.name}</p>
            <p className="text-xs text-neutral-500">{formatDate(folder.updatedAt)}</p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <DropdownMenu.Root>
            <DropdownMenu.Trigger className="p-2 rounded-lg hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500">
              <MoreVertical className="h-4 w-4 text-neutral-600" />
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
      </div>

      <ConfirmDeleteDialog
        open={showDeleteDialog}
        onOpenChange={setShowDeleteDialog}
        title="Delete folder"
        description={`Are you sure you want to delete "${folder.name}"?`}
        onConfirm={() => {
          onDelete(folder.id);
          setShowDeleteDialog(false);
        }}
        isDeleting={isDeleting}
      />
    </>
  );
}

function FileListItem({
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
          "group flex items-center gap-4 px-4 py-3 hover:bg-neutral-50 transition-colors",
          isSelected && "bg-primary-50"
        )}
      >
        {/* Selection Checkbox */}
        {selectionMode && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onToggleSelect();
            }}
            className={cn(
              "h-5 w-5 rounded border-2 flex items-center justify-center transition-all flex-shrink-0",
              isSelected
                ? "bg-primary-600 border-primary-600"
                : "bg-white border-neutral-300 hover:border-primary-500"
            )}
          >
            {isSelected && <Check className="h-3 w-3 text-white" />}
          </button>
        )}

        {/* File Icon & Info */}
        <div className="flex items-center gap-3 flex-1 min-w-0">
          <div className="h-10 w-10 rounded-lg bg-neutral-100 flex items-center justify-center flex-shrink-0">
            <FileText className="h-5 w-5 text-neutral-600" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-neutral-900 truncate">{document.filename}</p>
            <p className="text-xs text-neutral-500">
              {formatBytes(document.sizeBytes)} • {formatDate(document.updatedAt)}
            </p>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={() => onDownload(document)}
            className="p-2 rounded-lg hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500"
            title="Download"
          >
            <Download className="h-4 w-4 text-neutral-600" />
          </button>

          <DropdownMenu.Root>
            <DropdownMenu.Trigger className="p-2 rounded-lg hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500">
              <MoreVertical className="h-4 w-4 text-neutral-600" />
            </DropdownMenu.Trigger>

            <DropdownMenu.Portal>
              <DropdownMenu.Content
                className="min-w-[160px] bg-white rounded-xl p-2 shadow-xl border border-neutral-200"
                sideOffset={5}
              >
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

"use client";

import { useState } from "react";
import { FileText, Download, Trash2, Edit2, MoreVertical, Share2 } from "lucide-react";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { DocumentResponse } from "@/lib/documents-api";
import { formatBytes, formatDate } from "@/lib/utils";
import { ConfirmDeleteDialog } from "../common/ConfirmDeleteDialog";

interface FileRowProps {
  document: DocumentResponse;
  onDownload: () => void;
  onRename: () => void;
  onDelete: () => void;
  onShare?: () => void;
  isDeleting?: boolean;
}

export function FileRow({ document, onDownload, onRename, onDelete, onShare, isDeleting }: FileRowProps) {
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  return (
    <>
      <div className="group flex items-center gap-4 px-4 py-3 hover:bg-graphite-soft/5 transition-colors border-b border-hairline last:border-b-0">
        <div className="flex items-center gap-3 flex-1 min-w-0">
          <FileText className="h-5 w-5 text-graphite-soft flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-graphite truncate">{document.filename}</p>
            <p className="text-xs text-graphite-soft">
              {formatBytes(document.sizeBytes)} • {formatDate(document.updatedAt)}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={onDownload}
            className="p-2 rounded-lg hover:bg-graphite-soft/10 transition-colors"
            title="Download"
          >
            <Download className="h-4 w-4 text-graphite" />
          </button>

          <DropdownMenu.Root>
            <DropdownMenu.Trigger className="p-2 rounded-lg hover:bg-graphite-soft/10 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal">
              <MoreVertical className="h-4 w-4 text-graphite" />
            </DropdownMenu.Trigger>

            <DropdownMenu.Portal>
              <DropdownMenu.Content
                className="min-w-[160px] bg-paper rounded-lg p-1 shadow-xl border border-hairline"
                sideOffset={5}
              >
                <DropdownMenu.Item
                  onClick={onRename}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-graphite rounded-lg hover:bg-graphite-soft/10 cursor-pointer outline-none"
                >
                  <Edit2 className="h-4 w-4" />
                  Rename
                </DropdownMenu.Item>
                {onShare && (
                  <DropdownMenu.Item
                    onClick={onShare}
                    className="flex items-center gap-2 px-3 py-2 text-sm text-graphite rounded-lg hover:bg-graphite-soft/10 cursor-pointer outline-none"
                  >
                    <Share2 className="h-4 w-4" />
                    Share
                  </DropdownMenu.Item>
                )}
                <DropdownMenu.Item
                  onClick={() => setShowDeleteDialog(true)}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-brick rounded-lg hover:bg-brick/10 cursor-pointer outline-none"
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
        description={`Are you sure you want to delete "${document.filename}"? This action cannot be undone.`}
        onConfirm={() => {
          onDelete();
          setShowDeleteDialog(false);
        }}
        isDeleting={isDeleting}
      />
    </>
  );
}

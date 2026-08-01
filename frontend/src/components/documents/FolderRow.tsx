"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Folder, Trash2, MoreVertical } from "lucide-react";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { FolderResponse } from "@/lib/documents-api";
import { formatDate } from "@/lib/utils";
import { ConfirmDeleteDialog } from "../common/ConfirmDeleteDialog";

interface FolderRowProps {
  folder: FolderResponse;
  onDelete: () => void;
  isDeleting?: boolean;
}

export function FolderRow({ folder, onDelete, isDeleting }: FolderRowProps) {
  const router = useRouter();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  return (
    <>
      <div className="group flex items-center gap-4 px-4 py-3 hover:bg-graphite-soft/5 transition-colors border-b border-hairline last:border-b-0">
        <div
          className="flex items-center gap-3 flex-1 min-w-0 cursor-pointer"
          onClick={() => router.push(`/documents/${folder.id}`)}
        >
          <Folder className="h-5 w-5 text-teal flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-graphite truncate">{folder.name}</p>
            <p className="text-xs text-graphite-soft">{formatDate(folder.updatedAt)}</p>
          </div>
        </div>

        <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
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
        title="Delete folder"
        description={`Are you sure you want to delete "${folder.name}"? All files and subfolders will be deleted. This action cannot be undone.`}
        onConfirm={() => {
          onDelete();
          setShowDeleteDialog(false);
        }}
        isDeleting={isDeleting}
      />
    </>
  );
}

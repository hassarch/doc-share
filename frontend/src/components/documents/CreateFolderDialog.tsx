"use client";

import { useState } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { Button } from "../common/Button";
import { useCreateFolder } from "@/hooks/use-documents";
import { ApiError } from "@/lib/api";

interface CreateFolderDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  parentFolderId: string | null;
}

export function CreateFolderDialog({ open, onOpenChange, parentFolderId }: CreateFolderDialogProps) {
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const createMutation = useCreateFolder();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    try {
      setError(null);
      await createMutation.mutateAsync({ name, parentFolderId });
      setName("");
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create folder");
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
        <Dialog.Content className="fixed left-1/2 top-1/2 max-h-[85vh] w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-xl bg-paper p-6 shadow-2xl data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95">
          <div className="flex items-start justify-between mb-4">
            <Dialog.Title className="text-lg font-semibold text-graphite">
              Create new folder
            </Dialog.Title>
            <Dialog.Close className="rounded-lg p-1 hover:bg-graphite-soft/10 transition-colors">
              <X className="h-5 w-5 text-graphite-soft" />
            </Dialog.Close>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="mb-6">
              <label htmlFor="folderName" className="block text-sm font-medium text-graphite mb-2">
                Folder name
              </label>
              <input
                id="folderName"
                type="text"
                required
                autoFocus
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal"
                placeholder="My Documents"
              />
            </div>

            {error && (
              <div className="mb-4 p-3 rounded-lg bg-brick/10 border border-brick/20 text-sm text-brick">
                {error}
              </div>
            )}

            <div className="flex gap-3 justify-end">
              <Button
                type="button"
                variant="secondary"
                onClick={() => onOpenChange(false)}
                disabled={createMutation.isPending}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Creating..." : "Create"}
              </Button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

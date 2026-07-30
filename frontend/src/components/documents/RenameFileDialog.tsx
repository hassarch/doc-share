"use client";

import { useState, useEffect } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { Button } from "../common/Button";
import { DocumentResponse } from "@/lib/documents-api";

interface RenameFileDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  document: DocumentResponse;
  onRename: (id: string, filename: string) => void;
  isRenaming: boolean;
}

export function RenameFileDialog({
  open,
  onOpenChange,
  document,
  onRename,
  isRenaming,
}: RenameFileDialogProps) {
  const [filename, setFilename] = useState(document.filename);

  useEffect(() => {
    setFilename(document.filename);
  }, [document.filename]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    onRename(document.id, filename);
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
        <Dialog.Content className="fixed left-1/2 top-1/2 max-h-[85vh] w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-xl bg-paper p-6 shadow-2xl data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95">
          <div className="flex items-start justify-between mb-4">
            <Dialog.Title className="text-lg font-semibold text-graphite">
              Rename file
            </Dialog.Title>
            <Dialog.Close className="rounded-lg p-1 hover:bg-graphite-soft/10 transition-colors">
              <X className="h-5 w-5 text-graphite-soft" />
            </Dialog.Close>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="mb-6">
              <label htmlFor="filename" className="block text-sm font-medium text-graphite mb-2">
                New filename
              </label>
              <input
                id="filename"
                type="text"
                required
                autoFocus
                value={filename}
                onChange={(e) => setFilename(e.target.value)}
                className="w-full rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal"
              />
            </div>

            <div className="flex gap-3 justify-end">
              <Button
                type="button"
                variant="secondary"
                onClick={() => onOpenChange(false)}
                disabled={isRenaming}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isRenaming}>
                {isRenaming ? "Renaming..." : "Rename"}
              </Button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

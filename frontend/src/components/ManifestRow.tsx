"use client";

import { Folder as FolderIcon, FileText, Download, Trash2 } from "lucide-react";
import type { DocumentResponse, FolderResponse } from "@/lib/documents-api";

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const units = ["KB", "MB", "GB"];
  let value = bytes / 1024;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex++;
  }
  return `${value.toFixed(1)} ${units[unitIndex]}`;
}

const STATUS_LABEL: Record<DocumentResponse["replicationStatus"], string> = {
  PENDING: "Replication pending",
  REPLICATED: "Replicated",
  FAILED: "Replication failed",
};

const STATUS_COLOR: Record<DocumentResponse["replicationStatus"], string> = {
  PENDING: "bg-amber",
  REPLICATED: "bg-teal",
  FAILED: "bg-brick",
};

export function FolderRow({
  folder,
  onOpen,
  onDelete,
}: {
  folder: FolderResponse;
  onOpen: (folder: FolderResponse) => void;
  onDelete: (folder: FolderResponse) => void;
}) {
  return (
    <div className="group flex items-center gap-3 border-b border-hairline px-4 py-3 last:border-b-0 hover:bg-fog">
      <FolderIcon size={18} className="shrink-0 text-graphite-soft" aria-hidden />
      <button
        onClick={() => onOpen(folder)}
        className="flex-1 truncate text-left text-sm font-medium text-graphite hover:text-teal"
      >
        {folder.name}
      </button>
      <button
        onClick={() => onDelete(folder)}
        aria-label={`Delete folder ${folder.name}`}
        className="shrink-0 rounded p-1.5 text-graphite-soft opacity-0 transition-opacity hover:bg-brick/10 hover:text-brick group-hover:opacity-100"
      >
        <Trash2 size={16} />
      </button>
    </div>
  );
}

export function DocumentRow({
  document,
  onDownload,
  onDelete,
}: {
  document: DocumentResponse;
  onDownload: (document: DocumentResponse) => void;
  onDelete: (document: DocumentResponse) => void;
}) {
  const hashPrefix = document.sha256Hash.slice(0, 10);

  return (
    <div className="group flex items-center gap-3 border-b border-hairline px-4 py-3 last:border-b-0 hover:bg-fog">
      <FileText size={18} className="shrink-0 text-graphite-soft" aria-hidden />

      <span className="flex-1 truncate text-sm text-graphite">{document.filename}</span>

      {/* The manifest metadata cluster - this app's signature: content
          integrity data made visible instead of hidden plumbing. */}
      <div className="hidden shrink-0 items-center gap-4 font-mono text-xs text-graphite-soft sm:flex">
        <span className="w-16 text-right">{formatBytes(document.sizeBytes)}</span>
        <span title={document.sha256Hash}>{hashPrefix}</span>
        <span
          className="flex items-center gap-1.5"
          title={STATUS_LABEL[document.replicationStatus]}
        >
          <span
            className={`h-2 w-2 rounded-full ${STATUS_COLOR[document.replicationStatus]}`}
            aria-hidden
          />
          <span className="sr-only">{STATUS_LABEL[document.replicationStatus]}</span>
        </span>
      </div>

      <div className="flex shrink-0 items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
        <button
          onClick={() => onDownload(document)}
          aria-label={`Download ${document.filename}`}
          className="rounded p-1.5 text-graphite-soft hover:bg-teal-soft hover:text-teal"
        >
          <Download size={16} />
        </button>
        <button
          onClick={() => onDelete(document)}
          aria-label={`Delete ${document.filename}`}
          className="rounded p-1.5 text-graphite-soft hover:bg-brick/10 hover:text-brick"
        >
          <Trash2 size={16} />
        </button>
      </div>
    </div>
  );
}

"use client";

import { X, Trash2, Download, Share2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface BulkActionsToolbarProps {
  selectedCount: number;
  onClearSelection: () => void;
  onBulkDelete: () => void;
  onBulkDownload?: () => void;
  onBulkShare?: () => void;
}

export function BulkActionsToolbar({
  selectedCount,
  onClearSelection,
  onBulkDelete,
  onBulkDownload,
  onBulkShare,
}: BulkActionsToolbarProps) {
  if (selectedCount === 0) return null;

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 animate-in slide-in-from-bottom-4 fade-in duration-200">
      <div className="bg-neutral-900 text-white rounded-xl shadow-2xl border border-neutral-700 flex items-center gap-4 px-4 py-3">
        {/* Selection Count */}
        <div className="flex items-center gap-3 px-2">
          <span className="text-sm font-medium">
            {selectedCount} {selectedCount === 1 ? "item" : "items"} selected
          </span>
          <button
            onClick={onClearSelection}
            className="p-1 rounded-lg hover:bg-neutral-800 transition-colors focus:outline-none focus:ring-2 focus:ring-white"
            title="Clear selection"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Divider */}
        <div className="h-6 w-px bg-neutral-700"></div>

        {/* Actions */}
        <div className="flex items-center gap-1">
          {onBulkDownload && (
            <button
              onClick={onBulkDownload}
              className={cn(
                "flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium",
                "hover:bg-neutral-800 transition-colors",
                "focus:outline-none focus:ring-2 focus:ring-white"
              )}
              title="Download selected"
            >
              <Download className="h-4 w-4" />
              Download
            </button>
          )}

          {onBulkShare && (
            <button
              onClick={onBulkShare}
              className={cn(
                "flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium",
                "hover:bg-neutral-800 transition-colors",
                "focus:outline-none focus:ring-2 focus:ring-white"
              )}
              title="Share selected"
            >
              <Share2 className="h-4 w-4" />
              Share
            </button>
          )}

          <button
            onClick={onBulkDelete}
            className={cn(
              "flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium",
              "hover:bg-error-600 transition-colors",
              "focus:outline-none focus:ring-2 focus:ring-white"
            )}
            title="Delete selected"
          >
            <Trash2 className="h-4 w-4" />
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}

"use client";

import { useDocuments, useFolders } from "@/hooks/use-documents";
import { FileBrowser } from "@/components/documents/FileBrowser";
import { AlertCircle } from "lucide-react";

export default function DocumentsPage() {
  const { data: documents = [], isLoading: docsLoading, error: docsError } = useDocuments(null);
  const { data: folders = [], isLoading: foldersLoading, error: foldersError } = useFolders(null);

  if (docsLoading || foldersLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-graphite-soft">Loading...</div>
      </div>
    );
  }

  if (docsError || foldersError) {
    return (
      <div className="flex h-full items-center justify-center p-6">
        <div className="max-w-md text-center">
          <AlertCircle className="h-12 w-12 text-brick mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-graphite mb-2">Failed to load documents</h2>
          <p className="text-sm text-graphite-soft mb-4">
            {docsError?.message || foldersError?.message || "An unexpected error occurred"}
          </p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-teal text-white text-sm font-medium rounded-lg hover:opacity-90"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return <FileBrowser documents={documents} folders={folders} currentFolderId={null} />;
}

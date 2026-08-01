"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, AlertCircle } from "lucide-react";
import { useDocuments, useFolders } from "@/hooks/use-documents";
import { FileBrowser } from "@/components/documents/FileBrowser";
import { Button } from "@/components/common/Button";

export default function FolderPage({ params }: { params: Promise<{ folderId: string }> }) {
  const { folderId } = use(params);
  const router = useRouter();

  const { data: documents = [], isLoading: docsLoading, error: docsError } = useDocuments(folderId);
  const { data: folders = [], isLoading: foldersLoading, error: foldersError } = useFolders(folderId);

  if (docsLoading || foldersLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-graphite-soft">Loading...</div>
      </div>
    );
  }

  if (docsError || foldersError) {
    return (
      <div className="flex h-full flex-col">
        <div className="border-b border-hairline bg-paper px-6 py-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => router.back()}
          >
            <ChevronLeft className="h-4 w-4" />
            Back
          </Button>
        </div>
        <div className="flex-1 flex items-center justify-center p-6">
          <div className="max-w-md text-center">
            <AlertCircle className="h-12 w-12 text-brick mx-auto mb-4" />
            <h2 className="text-lg font-semibold text-graphite mb-2">Failed to load folder</h2>
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
      </div>
    );
  }

  return (
    <div>
      <div className="border-b border-hairline bg-paper px-6 py-4">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => router.back()}
        >
          <ChevronLeft className="h-4 w-4" />
          Back
        </Button>
      </div>
      <FileBrowser documents={documents} folders={folders} currentFolderId={folderId} />
    </div>
  );
}

"use client";

import { Upload, FolderPlus, MousePointerClick } from "lucide-react";

interface EnhancedEmptyStateProps {
  onUploadClick: () => void;
  onCreateFolderClick: () => void;
}

export function EnhancedEmptyState({ onUploadClick, onCreateFolderClick }: EnhancedEmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      {/* Illustration */}
      <div className="relative mb-8">
        <div className="h-32 w-32 rounded-full bg-primary-50 flex items-center justify-center">
          <FolderPlus className="h-16 w-16 text-primary-300" />
        </div>
        <div className="absolute -top-2 -right-2 h-12 w-12 rounded-full bg-primary-100 flex items-center justify-center animate-bounce">
          <Upload className="h-6 w-6 text-primary-600" />
        </div>
      </div>

      {/* Text Content */}
      <h3 className="text-xl font-semibold text-neutral-900 mb-2">
        No files yet
      </h3>
      <p className="text-neutral-600 text-center max-w-md mb-8">
        Get started by uploading your first file or creating a folder to organize your documents
      </p>

      {/* Action Buttons */}
      <div className="flex flex-col sm:flex-row items-center gap-3 mb-8">
        <button
          onClick={onUploadClick}
          className="flex items-center gap-2 px-6 py-3 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-lg transition-all duration-200 shadow-md hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 transform hover:scale-105"
        >
          <Upload className="h-5 w-5" />
          Upload File
        </button>
        <button
          onClick={onCreateFolderClick}
          className="flex items-center gap-2 px-6 py-3 bg-white hover:bg-neutral-50 text-neutral-700 font-medium border-2 border-neutral-300 rounded-lg transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
        >
          <FolderPlus className="h-5 w-5" />
          New Folder
        </button>
      </div>

      {/* Tips */}
      <div className="bg-primary-50 rounded-xl p-6 max-w-md border border-primary-100">
        <div className="flex items-start gap-3">
          <div className="h-8 w-8 rounded-lg bg-primary-100 flex items-center justify-center flex-shrink-0">
            <MousePointerClick className="h-4 w-4 text-primary-600" />
          </div>
          <div>
            <h4 className="font-medium text-primary-900 mb-1">Pro Tip</h4>
            <p className="text-sm text-primary-700">
              You can also drag and drop files anywhere on this page to upload them quickly!
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

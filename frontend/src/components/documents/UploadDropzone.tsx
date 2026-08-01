"use client";

import { useRef, useState, DragEvent } from "react";
import { Upload } from "lucide-react";
import { cn } from "@/lib/utils";

interface UploadDropzoneProps {
  onUpload: (file: File) => void;
  isUploading?: boolean;
}

export function UploadDropzone({ onUpload, isUploading }: UploadDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  function handleDragOver(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragging(true);
  }

  function handleDragLeave() {
    setIsDragging(false);
  }

  function handleDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setIsDragging(false);

    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      onUpload(files[0]);
    }
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const files = e.target.files;
    if (files && files.length > 0) {
      onUpload(files[0]);
    }
  }

  return (
    <div
      className={cn(
        "border-2 border-dashed rounded-lg p-8 transition-colors",
        isDragging ? "border-teal bg-teal/5" : "border-hairline hover:border-graphite-soft/50",
        isUploading && "opacity-60 pointer-events-none"
      )}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        onChange={handleFileSelect}
        disabled={isUploading}
      />

      <div className="flex flex-col items-center gap-3 text-center">
        <div
          className={cn(
            "h-12 w-12 rounded-full flex items-center justify-center transition-colors",
            isDragging ? "bg-teal/10" : "bg-graphite-soft/10"
          )}
        >
          <Upload className={cn("h-6 w-6", isDragging ? "text-teal" : "text-graphite-soft")} />
        </div>

        <div>
          <p className="text-sm font-medium text-graphite mb-1">
            {isUploading ? "Uploading..." : "Drop a file here or click to browse"}
          </p>
          <p className="text-xs text-graphite-soft">Any file type supported</p>
        </div>

        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={isUploading}
          className="px-4 py-2 bg-teal text-white text-sm font-medium rounded-lg hover:opacity-90 transition-opacity disabled:opacity-60"
        >
          Select file
        </button>
      </div>
    </div>
  );
}

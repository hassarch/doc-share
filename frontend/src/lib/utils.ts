import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/** Utility for merging Tailwind classes with proper precedence */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Format bytes to human-readable size */
export function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`;
}

/** Format ISO date string to relative time or absolute date */
export function formatDate(isoString: string): string {
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return "just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;

  return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

/** Get file icon name based on MIME type */
export function getFileIconType(mimeType: string): string {
  if (mimeType.startsWith("image/")) return "image";
  if (mimeType.startsWith("video/")) return "video";
  if (mimeType.startsWith("audio/")) return "audio";
  if (mimeType === "application/pdf") return "file-text";
  if (mimeType.includes("word")) return "file-text";
  if (mimeType.includes("spreadsheet") || mimeType.includes("excel")) return "sheet";
  if (mimeType.includes("presentation") || mimeType.includes("powerpoint")) return "presentation";
  if (mimeType.includes("zip") || mimeType.includes("archive")) return "archive";
  return "file";
}

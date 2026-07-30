import { apiFetch, apiUpload } from "./api";

export interface DocumentResponse {
  id: string;
  filename: string;
  folderId: string | null;
  sizeBytes: number;
  mimeType: string;
  sha256Hash: string;
  replicationStatus: "PENDING" | "REPLICATED" | "FAILED";
  createdAt: string;
  updatedAt: string;
}

export interface FolderResponse {
  id: string;
  name: string;
  parentFolderId: string | null;
  createdAt: string;
  updatedAt: string;
}

export async function listDocuments(folderId: string | null): Promise<DocumentResponse[]> {
  const query = folderId ? `?folderId=${folderId}` : "";
  return apiFetch<DocumentResponse[]>(`/api/v1/documents${query}`);
}

export async function listFolders(parentFolderId: string | null): Promise<FolderResponse[]> {
  const query = parentFolderId ? `?parentFolderId=${parentFolderId}` : "";
  return apiFetch<FolderResponse[]>(`/api/v1/folders${query}`);
}

export async function uploadDocument(
  file: File,
  folderId: string | null,
): Promise<DocumentResponse> {
  const formData = new FormData();
  formData.append("file", file);
  const query = folderId ? `?folderId=${folderId}` : "";
  return apiUpload<DocumentResponse>(`/api/v1/documents${query}`, formData);
}

export async function downloadDocument(id: string, filename: string): Promise<void> {
  const { getAccessToken } = await import("./api");
  const token = getAccessToken();
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

  const response = await fetch(`${base}/api/v1/documents/${id}/download`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) throw new Error("Download failed");

  // Trigger a browser download rather than navigating away from the app.
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}

export async function renameDocument(id: string, filename: string): Promise<DocumentResponse> {
  return apiFetch<DocumentResponse>(`/api/v1/documents/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ filename }),
  });
}

export async function deleteDocument(id: string): Promise<void> {
  return apiFetch<void>(`/api/v1/documents/${id}`, { method: "DELETE" });
}

export async function createFolder(
  name: string,
  parentFolderId: string | null,
): Promise<FolderResponse> {
  return apiFetch<FolderResponse>("/api/v1/folders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, parentFolderId }),
  });
}

export async function deleteFolder(id: string): Promise<void> {
  return apiFetch<void>(`/api/v1/folders/${id}`, { method: "DELETE" });
}

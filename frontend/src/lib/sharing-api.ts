import { apiFetch } from "./api";

export type ShareRole = "VIEWER" | "EDITOR" | "OWNER";

export interface ShareRequest {
  documentId: string;
  email: string;
  role: ShareRole;
}

export interface ShareResponse {
  id: string;
  documentId: string;
  userId: string;
  userEmail: string;
  userName: string;
  role: ShareRole;
  grantedAt: string;
}

export interface ShareLinkRequest {
  documentId: string;
  expiresAt?: string;
  password?: string;
  downloadLimit?: number;
  readOnly?: boolean;
}

export interface ShareLinkResponse {
  id: string;
  token: string;
  documentId: string;
  expiresAt: string | null;
  hasPassword: boolean;
  downloadLimit: number | null;
  downloadsUsed: number;
  readOnly: boolean;
  createdAt: string;
}

/** Share a document directly with another user by email */
export async function shareDocument(
  documentId: string,
  email: string,
  role: ShareRole
): Promise<ShareResponse> {
  return apiFetch<ShareResponse>("/api/v1/shares", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ documentId, email, role }),
  });
}

/** List all users a document is shared with */
export async function listShares(documentId: string): Promise<ShareResponse[]> {
  return apiFetch<ShareResponse[]>(`/api/v1/documents/${documentId}/shares`);
}

/** Revoke a user's access to a document */
export async function revokeShare(shareId: string): Promise<void> {
  return apiFetch<void>(`/api/v1/shares/${shareId}`, { method: "DELETE" });
}

/** Create a public share link */
export async function createShareLink(
  request: ShareLinkRequest
): Promise<ShareLinkResponse> {
  return apiFetch<ShareLinkResponse>("/api/v1/share-links", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

/** List all share links for a document */
export async function listShareLinks(documentId: string): Promise<ShareLinkResponse[]> {
  return apiFetch<ShareLinkResponse[]>(`/api/v1/documents/${documentId}/share-links`);
}

/** Delete a share link */
export async function deleteShareLink(linkId: string): Promise<void> {
  return apiFetch<void>(`/api/v1/share-links/${linkId}`, { method: "DELETE" });
}

/** Access a public share link (no auth required) */
export async function accessShareLink(
  token: string,
  password?: string
): Promise<{ documentId: string; filename: string; canDownload: boolean }> {
  return apiFetch<{ documentId: string; filename: string; canDownload: boolean }>(
    `/api/v1/share-links/${token}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password }),
      skipAuth: true,
    }
  );
}

/** Download via share link (no auth required) */
export async function downloadViaShareLink(
  token: string,
  filename: string,
  password?: string
): Promise<void> {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

  const response = await fetch(`${base}/api/v1/share-links/${token}/download`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ password }),
  });

  if (!response.ok) throw new Error("Download failed");

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}

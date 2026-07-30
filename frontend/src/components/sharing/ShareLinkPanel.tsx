"use client";

import { useState } from "react";
import { Link as LinkIcon, Copy, Trash2, Check, Lock, Calendar, Download } from "lucide-react";
import { DocumentResponse } from "@/lib/documents-api";
import { useShareLinks, useCreateShareLink, useDeleteShareLink } from "@/hooks/use-sharing";
import { Button } from "../common/Button";
import { formatDate } from "@/lib/utils";
import { ApiError } from "@/lib/api";

interface ShareLinkPanelProps {
  document: DocumentResponse;
}

export function ShareLinkPanel({ document }: ShareLinkPanelProps) {
  const [expiresInDays, setExpiresInDays] = useState<number | "">(7);
  const [password, setPassword] = useState("");
  const [downloadLimit, setDownloadLimit] = useState<number | "">("");
  const [readOnly, setReadOnly] = useState(true);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: links = [], error: linksError } = useShareLinks(document.id);
  const createMutation = useCreateShareLink();
  const deleteMutation = useDeleteShareLink();

  async function handleCreate() {
    try {
      setError(null);
      const expiresAt =
        expiresInDays !== ""
          ? new Date(Date.now() + expiresInDays * 24 * 60 * 60 * 1000).toISOString()
          : undefined;

      await createMutation.mutateAsync({
        documentId: document.id,
        expiresAt,
        password: password || undefined,
        downloadLimit: downloadLimit || undefined,
        readOnly,
      });

      // Reset form
      setExpiresInDays(7);
      setPassword("");
      setDownloadLimit("");
      setReadOnly(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create link");
    }
  }

  async function handleDelete(linkId: string) {
    try {
      setError(null);
      await deleteMutation.mutateAsync(linkId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete link");
    }
  }

  function copyLink(token: string, linkId: string) {
    const url = `${window.location.origin}/share/${token}`;
    navigator.clipboard.writeText(url);
    setCopiedId(linkId);
    setTimeout(() => setCopiedId(null), 2000);
  }

  return (
    <div>
      {/* Backend not implemented notice */}
      <div className="mb-4 p-4 rounded-lg bg-yellow-50 border border-yellow-200">
        <p className="text-sm font-medium text-yellow-800 mb-1">
          ⚠️ Share Links Feature Coming Soon
        </p>
        <p className="text-xs text-yellow-700">
          The share link API endpoints are not yet implemented in the backend. 
          This is a Phase 0 UI demonstration. Backend implementation coming in Phase 0 completion.
        </p>
      </div>

      {/* Error message */}
      {(error || linksError) && (
        <div className="mb-4 p-3 rounded-lg bg-brick/10 border border-brick/20 text-sm text-brick">
          {error || linksError?.message || "An error occurred"}
        </div>
      )}

      {/* Create link form */}
      <div className="bg-graphite-soft/5 rounded-lg p-4 mb-6">
        <h3 className="text-sm font-medium text-graphite mb-4">Create new share link</h3>

        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-graphite mb-2">
                <Calendar className="inline h-3 w-3 mr-1" />
                Expires in (days)
              </label>
              <input
                type="number"
                min="1"
                value={expiresInDays}
                onChange={(e) => setExpiresInDays(e.target.value ? Number(e.target.value) : "")}
                placeholder="Never"
                className="w-full rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-graphite mb-2">
                <Download className="inline h-3 w-3 mr-1" />
                Download limit
              </label>
              <input
                type="number"
                min="1"
                value={downloadLimit}
                onChange={(e) => setDownloadLimit(e.target.value ? Number(e.target.value) : "")}
                placeholder="Unlimited"
                className="w-full rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-graphite mb-2">
              <Lock className="inline h-3 w-3 mr-1" />
              Password (optional)
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Leave empty for no password"
              className="w-full rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal"
            />
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={readOnly}
              onChange={(e) => setReadOnly(e.target.checked)}
              className="h-4 w-4 rounded border-hairline text-teal focus:ring-teal"
            />
            <span className="text-sm text-graphite">Read-only (prevent downloads)</span>
          </label>

          <Button
            onClick={handleCreate}
            disabled={createMutation.isPending}
            className="w-full"
          >
            <LinkIcon className="h-4 w-4" />
            {createMutation.isPending ? "Creating..." : "Create link"}
          </Button>
        </div>
      </div>

      {/* Existing links */}
      {links.length > 0 && (
        <div>
          <h3 className="text-sm font-medium text-graphite mb-3">Active links</h3>
          <div className="space-y-2">
            {links.map((link) => (
              <div
                key={link.id}
                className="flex items-start gap-3 p-3 rounded-lg border border-hairline hover:bg-graphite-soft/5 transition-colors"
              >
                <LinkIcon className="h-4 w-4 text-teal mt-1 flex-shrink-0" />

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    {link.hasPassword && (
                      <span className="px-1.5 py-0.5 text-xs bg-yellow-50 text-yellow-700 border border-yellow-200 rounded">
                        Password
                      </span>
                    )}
                    {link.readOnly && (
                      <span className="px-1.5 py-0.5 text-xs bg-gray-50 text-gray-700 border border-gray-200 rounded">
                        Read-only
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-graphite-soft font-mono truncate">
                    {window.location.origin}/share/{link.token}
                  </p>
                  <div className="flex gap-3 text-xs text-graphite-soft mt-1">
                    {link.expiresAt && (
                      <span>Expires {formatDate(link.expiresAt)}</span>
                    )}
                    {link.downloadLimit && (
                      <span>
                        {link.downloadCount}/{link.downloadLimit} downloads
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex gap-1">
                  <button
                    onClick={() => copyLink(link.token, link.id)}
                    className="p-2 rounded-lg hover:bg-graphite-soft/10 transition-colors"
                    title="Copy link"
                  >
                    {copiedId === link.id ? (
                      <Check className="h-4 w-4 text-green-600" />
                    ) : (
                      <Copy className="h-4 w-4 text-graphite" />
                    )}
                  </button>
                  <button
                    onClick={() => handleDelete(link.id)}
                    disabled={deleteMutation.isPending}
                    className="p-2 rounded-lg hover:bg-brick/10 text-brick transition-colors disabled:opacity-60"
                    title="Delete link"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

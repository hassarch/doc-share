"use client";

import { use, useState } from "react";
import { FileText, Download, Lock, AlertCircle } from "lucide-react";
import { accessShareLink, downloadViaShareLink } from "@/lib/sharing-api";
import { Button } from "@/components/common/Button";
import { ApiError } from "@/lib/api";

export default function ShareLinkPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = use(params);
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [linkInfo, setLinkInfo] = useState<{
    documentId: string;
    filename: string;
    canDownload: boolean;
  } | null>(null);

  async function handleAccess(e?: React.FormEvent) {
    e?.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const info = await accessShareLink(token, password || undefined);
      setLinkInfo(info);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 401) {
          setError("Incorrect password");
        } else if (err.status === 404) {
          setError("This link doesn't exist or has expired");
        } else if (err.status === 429) {
          setError("Download limit reached");
        } else {
          setError(err.message);
        }
      } else {
        setError("Failed to access the shared file");
      }
    } finally {
      setIsLoading(false);
    }
  }

  async function handleDownload() {
    setError(null);
    setIsLoading(true);

    try {
      if (!linkInfo) return;
      await downloadViaShareLink(token, linkInfo.filename, password || undefined);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Download failed");
      }
    } finally {
      setIsLoading(false);
    }
  }

  // Auto-access on mount if no password required
  useState(() => {
    handleAccess();
  });

  return (
    <div className="flex min-h-screen items-center justify-center bg-ink px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <span className="font-display text-3xl font-semibold tracking-tight text-mist">
            docshare
          </span>
        </div>

        <div className="rounded-xl bg-paper p-8 shadow-xl shadow-black/20">
          {!linkInfo ? (
            <>
              <div className="flex items-center gap-3 mb-6">
                <div className="h-12 w-12 rounded-full bg-teal/10 flex items-center justify-center">
                  <Lock className="h-6 w-6 text-teal" />
                </div>
                <div>
                  <h1 className="text-lg font-semibold text-graphite">Protected file</h1>
                  <p className="text-sm text-graphite-soft">Enter password to view</p>
                </div>
              </div>

              <form onSubmit={handleAccess}>
                <div className="mb-4">
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter password"
                    className="w-full rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal"
                    autoFocus
                  />
                </div>

                {error && (
                  <div className="mb-4 flex items-start gap-2 p-3 rounded-lg bg-brick/10 border border-brick/20">
                    <AlertCircle className="h-4 w-4 text-brick mt-0.5 flex-shrink-0" />
                    <p className="text-sm text-brick">{error}</p>
                  </div>
                )}

                <Button type="submit" disabled={isLoading} className="w-full">
                  {isLoading ? "Verifying..." : "Access file"}
                </Button>
              </form>
            </>
          ) : (
            <>
              <div className="flex items-center gap-3 mb-6">
                <div className="h-12 w-12 rounded-full bg-teal/10 flex items-center justify-center">
                  <FileText className="h-6 w-6 text-teal" />
                </div>
                <div className="flex-1 min-w-0">
                  <h1 className="text-lg font-semibold text-graphite truncate">
                    {linkInfo.filename}
                  </h1>
                  <p className="text-sm text-graphite-soft">Shared file</p>
                </div>
              </div>

              {error && (
                <div className="mb-4 flex items-start gap-2 p-3 rounded-lg bg-brick/10 border border-brick/20">
                  <AlertCircle className="h-4 w-4 text-brick mt-0.5 flex-shrink-0" />
                  <p className="text-sm text-brick">{error}</p>
                </div>
              )}

              {linkInfo.canDownload ? (
                <Button onClick={handleDownload} disabled={isLoading} className="w-full">
                  <Download className="h-4 w-4" />
                  {isLoading ? "Downloading..." : "Download"}
                </Button>
              ) : (
                <div className="text-center py-4">
                  <p className="text-sm text-graphite-soft mb-2">
                    This is a read-only share link
                  </p>
                  <p className="text-xs text-graphite-soft">
                    Preview only, downloads are disabled
                  </p>
                </div>
              )}
            </>
          )}
        </div>

        <p className="mt-6 text-center text-xs text-mist">
          Powered by <span className="font-medium">docshare</span>
        </p>
      </div>
    </div>
  );
}

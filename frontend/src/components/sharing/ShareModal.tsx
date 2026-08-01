"use client";

import { useState } from "react";
import * as Dialog from "@radix-ui/react-dialog";
import * as Select from "@radix-ui/react-select";
import { X, UserPlus, Check, ChevronDown } from "lucide-react";
import { Button } from "../common/Button";
import { DocumentResponse } from "@/lib/documents-api";
import { ShareRole } from "@/lib/sharing-api";
import { useShareDocument, useShares, useRevokeShare } from "@/hooks/use-sharing";
import { PermissionsList } from "./PermissionsList";
import { ShareLinkPanel } from "./ShareLinkPanel";
import { ApiError } from "@/lib/api";

interface ShareModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  document: DocumentResponse;
}

const roles: { value: ShareRole; label: string }[] = [
  { value: "VIEWER", label: "Viewer (read only)" },
  { value: "EDITOR", label: "Editor (can edit)" },
];

export function ShareModal({ open, onOpenChange, document }: ShareModalProps) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<ShareRole>("VIEWER");
  const [activeTab, setActiveTab] = useState<"direct" | "link">("direct");
  const [error, setError] = useState<string | null>(null);

  const shareMutation = useShareDocument();
  const { data: shares = [], error: sharesError } = useShares(document.id);
  const revokeMutation = useRevokeShare();

  async function handleShare(e: React.FormEvent) {
    e.preventDefault();
    try {
      setError(null);
      await shareMutation.mutateAsync({ documentId: document.id, email, role });
      setEmail("");
      setRole("VIEWER");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to share document");
    }
  }

  async function handleRevoke(shareId: string) {
    try {
      setError(null);
      await revokeMutation.mutateAsync(shareId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to revoke access");
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
        <Dialog.Content className="fixed left-1/2 top-1/2 max-h-[85vh] w-full max-w-2xl -translate-x-1/2 -translate-y-1/2 rounded-xl bg-paper p-6 shadow-2xl data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 overflow-y-auto">
          <div className="flex items-start justify-between mb-4">
            <div>
              <Dialog.Title className="text-lg font-semibold text-graphite">
                Share &quot;{document.filename}&quot;
              </Dialog.Title>
              <Dialog.Description className="text-sm text-graphite-soft mt-1">
                Collaborate with others or create a public link
              </Dialog.Description>
            </div>
            <Dialog.Close className="rounded-lg p-1 hover:bg-graphite-soft/10 transition-colors">
              <X className="h-5 w-5 text-graphite-soft" />
            </Dialog.Close>
          </div>

          {/* Tabs */}
          <div className="flex gap-1 border-b border-hairline mb-6">
            <button
              onClick={() => setActiveTab("direct")}
              className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
                activeTab === "direct"
                  ? "border-teal text-teal"
                  : "border-transparent text-graphite-soft hover:text-graphite"
              }`}
            >
              Direct share
            </button>
            <button
              onClick={() => setActiveTab("link")}
              className={`px-4 py-2 text-sm font-medium transition-colors border-b-2 ${
                activeTab === "link"
                  ? "border-teal text-teal"
                  : "border-transparent text-graphite-soft hover:text-graphite"
              }`}
            >
              Share link
            </button>
          </div>

          {activeTab === "direct" ? (
            <>
              {/* Error message */}
              {(error || sharesError) && (
                <div className="mb-4 p-3 rounded-lg bg-brick/10 border border-brick/20 text-sm text-brick">
                  {error || sharesError?.message || "An error occurred"}
                </div>
              )}

              {/* Share form */}
              <form onSubmit={handleShare} className="mb-6">
                <div className="flex gap-3">
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="user@example.com"
                    className="flex-1 rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal"
                  />

                  <Select.Root value={role} onValueChange={(v) => setRole(v as ShareRole)}>
                    <Select.Trigger className="flex items-center gap-2 rounded-lg border border-hairline bg-paper px-3 py-2 text-sm text-graphite outline-none focus:border-teal min-w-[180px]">
                      <Select.Value />
                      <Select.Icon>
                        <ChevronDown className="h-4 w-4" />
                      </Select.Icon>
                    </Select.Trigger>
                    <Select.Portal>
                      <Select.Content className="bg-paper rounded-lg border border-hairline shadow-xl overflow-hidden">
                        <Select.Viewport>
                          {roles.map((r) => (
                            <Select.Item
                              key={r.value}
                              value={r.value}
                              className="flex items-center gap-2 px-3 py-2 text-sm text-graphite cursor-pointer outline-none hover:bg-graphite-soft/10 data-[state=checked]:bg-teal/10"
                            >
                              <Select.ItemIndicator>
                                <Check className="h-4 w-4 text-teal" />
                              </Select.ItemIndicator>
                              <Select.ItemText>{r.label}</Select.ItemText>
                            </Select.Item>
                          ))}
                        </Select.Viewport>
                      </Select.Content>
                    </Select.Portal>
                  </Select.Root>

                  <Button type="submit" disabled={shareMutation.isPending}>
                    <UserPlus className="h-4 w-4" />
                    {shareMutation.isPending ? "Sharing..." : "Share"}
                  </Button>
                </div>
              </form>

              {/* Permissions list */}
              <PermissionsList
                shares={shares}
                onRevoke={handleRevoke}
                isRevoking={revokeMutation.isPending}
              />
            </>
          ) : (
            <ShareLinkPanel document={document} />
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

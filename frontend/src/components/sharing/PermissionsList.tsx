"use client";

import { User, Trash2 } from "lucide-react";
import { ShareResponse } from "@/lib/sharing-api";
import { formatDate } from "@/lib/utils";

interface PermissionsListProps {
  shares: ShareResponse[];
  onRevoke: (shareId: string) => void;
  isRevoking: boolean;
}

const roleBadgeColors = {
  VIEWER: "bg-blue-50 text-blue-700 border-blue-200",
  EDITOR: "bg-green-50 text-green-700 border-green-200",
  OWNER: "bg-purple-50 text-purple-700 border-purple-200",
};

export function PermissionsList({ shares, onRevoke, isRevoking }: PermissionsListProps) {
  if (shares.length === 0) {
    return (
      <div className="text-center py-8 text-sm text-graphite-soft">
        Not shared with anyone yet
      </div>
    );
  }

  return (
    <div>
      <h3 className="text-sm font-medium text-graphite mb-3">Shared with</h3>
      <div className="space-y-2">
        {shares.map((share) => (
          <div
            key={share.id}
            className="flex items-center justify-between p-3 rounded-lg border border-hairline hover:bg-graphite-soft/5 transition-colors"
          >
            <div className="flex items-center gap-3 flex-1 min-w-0">
              <div className="h-8 w-8 rounded-full bg-teal/10 flex items-center justify-center flex-shrink-0">
                <User className="h-4 w-4 text-teal" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-graphite truncate">
                  {share.userEmail}
                </p>
                <p className="text-xs text-graphite-soft">
                  Shared {formatDate(share.grantedAt)}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <span
                className={`px-2 py-1 text-xs font-medium rounded border ${
                  roleBadgeColors[share.role]
                }`}
              >
                {share.role}
              </span>
              <button
                onClick={() => onRevoke(share.id)}
                disabled={isRevoking}
                className="p-2 rounded-lg hover:bg-brick/10 text-brick transition-colors disabled:opacity-60"
                title="Revoke access"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

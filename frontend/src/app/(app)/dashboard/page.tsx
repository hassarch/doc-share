"use client";

import { FileText, Users, Clock } from "lucide-react";

export default function DashboardPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold text-graphite mb-6">Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <StatCard
          icon={<FileText className="h-6 w-6" />}
          label="My Documents"
          value="—"
          description="Phase 0: basic stats coming soon"
        />
        <StatCard
          icon={<Users className="h-6 w-6" />}
          label="Shared with me"
          value="—"
          description="Phase 0: sharing tracking coming"
        />
        <StatCard
          icon={<Clock className="h-6 w-6" />}
          label="Recent activity"
          value="—"
          description="Phase 3: activity feed coming"
        />
      </div>

      <div className="bg-paper rounded-lg border border-hairline p-8 text-center">
        <h2 className="text-lg font-semibold text-graphite mb-2">Welcome to docshare</h2>
        <p className="text-sm text-graphite-soft mb-6">
          Start by uploading documents or creating folders in <strong>My Documents</strong>.
        </p>
        <p className="text-xs text-graphite-soft">
          Phase 0 complete: Auth, file browser, upload, download, folders, and basic sharing.
        </p>
      </div>
    </div>
  );
}

function StatCard({
  icon,
  label,
  value,
  description,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  description: string;
}) {
  return (
    <div className="bg-paper rounded-lg border border-hairline p-6">
      <div className="flex items-center gap-3 mb-3">
        <div className="h-10 w-10 rounded-lg bg-teal/10 flex items-center justify-center text-teal">
          {icon}
        </div>
        <div>
          <p className="text-sm text-graphite-soft">{label}</p>
          <p className="text-2xl font-semibold text-graphite">{value}</p>
        </div>
      </div>
      <p className="text-xs text-graphite-soft">{description}</p>
    </div>
  );
}

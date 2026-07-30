"use client";

import { Users } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";

export default function SharedPage() {
  // Phase 0: sharing UI placeholder - backend support coming in Phase 0 completion
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold text-graphite mb-6">Shared with me</h1>
      <EmptyState
        icon={<Users className="h-12 w-12" />}
        title="No shared documents yet"
        description="Documents that others share with you will appear here"
      />
    </div>
  );
}

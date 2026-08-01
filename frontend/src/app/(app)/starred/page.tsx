"use client";

import { Star } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";

export default function StarredPage() {
  // Phase 4: favorites/starring feature
  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold text-graphite mb-6">Starred</h1>
      <EmptyState
        icon={<Star className="h-12 w-12" />}
        title="No starred documents"
        description="Star your important documents for quick access"
      />
    </div>
  );
}

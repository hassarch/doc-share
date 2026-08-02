"use client";

import { Grid3x3, List, Table2 } from "lucide-react";
import { cn } from "@/lib/utils";

export type ViewMode = "grid" | "list" | "table";

interface ViewSwitcherProps {
  currentView: ViewMode;
  onViewChange: (view: ViewMode) => void;
}

export function ViewSwitcher({ currentView, onViewChange }: ViewSwitcherProps) {
  const views: { mode: ViewMode; icon: React.ReactNode; label: string }[] = [
    { mode: "grid", icon: <Grid3x3 className="h-4 w-4" />, label: "Grid view" },
    { mode: "list", icon: <List className="h-4 w-4" />, label: "List view" },
    { mode: "table", icon: <Table2 className="h-4 w-4" />, label: "Table view" },
  ];

  return (
    <div className="flex items-center gap-1 bg-neutral-100 p-1 rounded-lg">
      {views.map((view) => (
        <button
          key={view.mode}
          onClick={() => onViewChange(view.mode)}
          className={cn(
            "p-2 rounded-md transition-all duration-200",
            "focus:outline-none focus:ring-2 focus:ring-primary-500",
            currentView === view.mode
              ? "bg-white text-primary-600 shadow-sm"
              : "text-neutral-600 hover:text-neutral-900 hover:bg-neutral-50"
          )}
          title={view.label}
          aria-label={view.label}
          aria-pressed={currentView === view.mode}
        >
          {view.icon}
        </button>
      ))}
    </div>
  );
}

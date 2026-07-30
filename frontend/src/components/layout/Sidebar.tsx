"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Folder, Users, Star } from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", icon: Home, label: "Dashboard" },
  { href: "/documents", icon: Folder, label: "My Documents" },
  { href: "/shared", icon: Users, label: "Shared with me" },
  { href: "/starred", icon: Star, label: "Starred" },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 bg-paper border-r border-hairline flex flex-col">
      <div className="p-6 border-b border-hairline">
        <Link href="/dashboard">
          <span className="font-display text-2xl font-semibold tracking-tight text-graphite">
            docshare
          </span>
        </Link>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href || pathname.startsWith(item.href + "/");

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors",
                isActive
                  ? "bg-teal/10 text-teal"
                  : "text-graphite hover:bg-graphite-soft/10"
              )}
            >
              <Icon className="h-5 w-5" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}

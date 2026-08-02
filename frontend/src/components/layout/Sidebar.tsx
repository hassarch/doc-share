"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { 
  Home, 
  Folder, 
  Users, 
  Star,
  FileText,
  ChevronLeft,
  ChevronRight,
  Plus,
  Upload
} from "lucide-react";
import { cn } from "@/lib/utils";

const navSections = [
  {
    title: "Main",
    items: [
      { href: "/dashboard", icon: Home, label: "Dashboard" },
      { href: "/documents", icon: Folder, label: "My Documents" },
    ],
  },
  {
    title: "Shared",
    items: [
      { href: "/shared", icon: Users, label: "Shared with me" },
      { href: "/starred", icon: Star, label: "Starred" },
    ],
  },
];

interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export function Sidebar({ isOpen = true, onClose }: SidebarProps) {
  const pathname = usePathname();
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <>
      {/* Mobile Overlay */}
      {isOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-black/50 z-40 transition-opacity"
          onClick={onClose}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          "fixed lg:static inset-y-0 left-0 z-50",
          "bg-white border-r border-neutral-200 flex flex-col",
          "transition-all duration-300 ease-in-out",
          isCollapsed ? "w-20" : "w-64",
          !isOpen && "lg:flex -translate-x-full lg:translate-x-0"
        )}
      >
        {/* Logo Section */}
        <div className={cn(
          "h-16 border-b border-neutral-200 flex items-center",
          "transition-all duration-300",
          isCollapsed ? "justify-center px-4" : "justify-between px-6"
        )}>
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="h-8 w-8 bg-primary-600 rounded-lg flex items-center justify-center flex-shrink-0">
              <FileText className="h-5 w-5 text-white" />
            </div>
            {!isCollapsed && (
              <span className="font-display text-xl font-semibold text-neutral-900">
                docshare
              </span>
            )}
          </Link>
          
          {/* Desktop Collapse Toggle */}
          <button
            onClick={() => setIsCollapsed(!isCollapsed)}
            className={cn(
              "hidden lg:flex p-1.5 rounded-lg hover:bg-neutral-100 transition-colors",
              "focus:outline-none focus:ring-2 focus:ring-primary-500",
              isCollapsed && "absolute -right-3 top-4 bg-white border border-neutral-200 shadow-md"
            )}
            aria-label={isCollapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {isCollapsed ? (
              <ChevronRight className="h-4 w-4 text-neutral-600" />
            ) : (
              <ChevronLeft className="h-4 w-4 text-neutral-600" />
            )}
          </button>

          {/* Mobile Close Button */}
          <button
            onClick={onClose}
            className="lg:hidden p-1.5 rounded-lg hover:bg-neutral-100 transition-colors"
            aria-label="Close menu"
          >
            <ChevronLeft className="h-5 w-5 text-neutral-600" />
          </button>
        </div>

        {/* Quick Actions */}
        {!isCollapsed && (
          <div className="p-4 border-b border-neutral-200">
            <button className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-lg transition-all duration-200 shadow-sm hover:shadow-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2">
              <Upload className="h-4 w-4" />
              Upload File
            </button>
            <button className="mt-2 w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-white hover:bg-neutral-50 text-neutral-700 font-medium border border-neutral-300 rounded-lg transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2">
              <Plus className="h-4 w-4" />
              New Folder
            </button>
          </div>
        )}

        {/* Collapsed Quick Actions */}
        {isCollapsed && (
          <div className="p-2 border-b border-neutral-200 space-y-2">
            <button
              className="w-full p-2.5 bg-primary-600 hover:bg-primary-700 text-white rounded-lg transition-colors shadow-sm hover:shadow-md focus:outline-none focus:ring-2 focus:ring-primary-500"
              title="Upload File"
            >
              <Upload className="h-4 w-4 mx-auto" />
            </button>
            <button
              className="w-full p-2.5 bg-white hover:bg-neutral-50 text-neutral-700 border border-neutral-300 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500"
              title="New Folder"
            >
              <Plus className="h-4 w-4 mx-auto" />
            </button>
          </div>
        )}

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto p-3">
          {navSections.map((section) => (
            <div key={section.title} className="mb-6 last:mb-0">
              {/* Section Title */}
              {!isCollapsed && (
                <h3 className="px-3 mb-2 text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                  {section.title}
                </h3>
              )}
              {isCollapsed && (
                <div className="mb-2 border-t border-neutral-200"></div>
              )}

              {/* Section Items */}
              <div className="space-y-1">
                {section.items.map((item) => {
                  const Icon = item.icon;
                  const isActive = pathname === item.href || pathname.startsWith(item.href + "/");

                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={cn(
                        "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200",
                        "relative group",
                        isActive
                          ? "bg-primary-50 text-primary-700 shadow-sm"
                          : "text-neutral-700 hover:bg-neutral-100 hover:text-neutral-900",
                        isCollapsed && "justify-center"
                      )}
                      title={isCollapsed ? item.label : undefined}
                    >
                      <Icon className={cn(
                        "h-5 w-5 flex-shrink-0",
                        isActive ? "text-primary-600" : "text-neutral-500 group-hover:text-neutral-700"
                      )} />
                      {!isCollapsed && (
                        <span className="truncate">{item.label}</span>
                      )}
                      {isActive && !isCollapsed && (
                        <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-primary-600 rounded-r-full"></div>
                      )}
                      {isActive && isCollapsed && (
                        <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-primary-600 rounded-r-full"></div>
                      )}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Storage Info (when expanded) */}
        {!isCollapsed && (
          <div className="p-4 border-t border-neutral-200">
            <div className="bg-neutral-50 rounded-lg p-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-neutral-700">Storage</span>
                <span className="text-xs text-neutral-600">2.1 GB / 5 GB</span>
              </div>
              <div className="h-2 bg-neutral-200 rounded-full overflow-hidden">
                <div className="h-full bg-primary-600 rounded-full" style={{ width: "42%" }}></div>
              </div>
              <p className="text-xs text-neutral-600 mt-2">
                42% used
              </p>
            </div>
          </div>
        )}
      </aside>
    </>
  );
}

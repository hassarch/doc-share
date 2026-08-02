"use client";

import { useState } from "react";
import { 
  LogOut, 
  User, 
  Settings, 
  HelpCircle, 
  Bell, 
  Search,
  Menu,
  ChevronRight
} from "lucide-react";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useAuth } from "@/context/AuthContext";
import { usePathname } from "next/navigation";
import Link from "next/link";

interface TopbarProps {
  onMenuClick?: () => void;
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { logout } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");
  const pathname = usePathname();

  // Generate breadcrumbs from pathname
  const breadcrumbs = generateBreadcrumbs(pathname);

  return (
    <header className="h-16 border-b border-neutral-200 bg-white flex items-center justify-between px-4 md:px-6 shadow-sm sticky top-0 z-30">
      {/* Left Section - Menu + Breadcrumbs */}
      <div className="flex items-center gap-4 flex-1 min-w-0">
        {/* Mobile Menu Button */}
        <button
          onClick={onMenuClick}
          className="lg:hidden p-2 rounded-lg hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500"
          aria-label="Toggle menu"
        >
          <Menu className="h-5 w-5 text-neutral-700" />
        </button>

        {/* Breadcrumbs */}
        <nav className="hidden md:flex items-center gap-2 text-sm min-w-0" aria-label="Breadcrumb">
          {breadcrumbs.map((crumb, index) => (
            <div key={crumb.href} className="flex items-center gap-2 min-w-0">
              {index > 0 && (
                <ChevronRight className="h-4 w-4 text-neutral-400 flex-shrink-0" />
              )}
              {index === breadcrumbs.length - 1 ? (
                <span className="font-medium text-neutral-900 truncate">
                  {crumb.label}
                </span>
              ) : (
                <Link
                  href={crumb.href}
                  className="text-neutral-600 hover:text-neutral-900 transition-colors truncate"
                >
                  {crumb.label}
                </Link>
              )}
            </div>
          ))}
        </nav>
      </div>

      {/* Center Section - Search Bar */}
      <div className="hidden lg:flex flex-1 max-w-md mx-4">
        <div className="relative w-full">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-neutral-400" />
          <input
            type="text"
            placeholder="Search documents..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-neutral-50 border border-neutral-200 rounded-lg
                     text-sm text-neutral-900 placeholder:text-neutral-500
                     focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
                     transition-all duration-200"
          />
          {searchQuery && (
            <kbd className="absolute right-3 top-1/2 -translate-y-1/2 px-2 py-1 text-xs font-semibold text-neutral-500 bg-neutral-100 border border-neutral-200 rounded">
              ⌘K
            </kbd>
          )}
        </div>
      </div>

      {/* Right Section - Actions */}
      <div className="flex items-center gap-2 md:gap-3">
        {/* Mobile Search Button */}
        <button
          className="lg:hidden p-2 rounded-lg hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500"
          aria-label="Search"
        >
          <Search className="h-5 w-5 text-neutral-700" />
        </button>

        {/* Notifications */}
        <DropdownMenu.Root>
          <DropdownMenu.Trigger className="relative p-2 rounded-lg hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500">
            <Bell className="h-5 w-5 text-neutral-700" />
            {/* Notification badge */}
            <span className="absolute top-1 right-1 h-2 w-2 bg-error-500 rounded-full ring-2 ring-white"></span>
          </DropdownMenu.Trigger>

          <DropdownMenu.Portal>
            <DropdownMenu.Content
              className="min-w-[320px] bg-white rounded-xl p-2 shadow-xl border border-neutral-200 animate-in fade-in slide-in-from-top-2"
              sideOffset={8}
              align="end"
            >
              <div className="px-3 py-2 border-b border-neutral-100">
                <h3 className="font-semibold text-sm text-neutral-900">Notifications</h3>
              </div>
              <div className="py-2">
                <div className="px-3 py-6 text-center">
                  <Bell className="h-8 w-8 text-neutral-300 mx-auto mb-2" />
                  <p className="text-sm text-neutral-600">No new notifications</p>
                  <p className="text-xs text-neutral-500 mt-1">You&apos;re all caught up!</p>
                </div>
              </div>
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>

        {/* Help */}
        <button
          className="hidden md:flex p-2 rounded-lg hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500"
          aria-label="Help"
        >
          <HelpCircle className="h-5 w-5 text-neutral-700" />
        </button>

        {/* User Menu */}
        <DropdownMenu.Root>
          <DropdownMenu.Trigger className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-neutral-100 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500">
            <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center ring-2 ring-white shadow-sm">
              <User className="h-4 w-4 text-primary-700" />
            </div>
            <div className="hidden md:block text-left">
              <p className="text-sm font-medium text-neutral-900 leading-none">
                User
              </p>
              <p className="text-xs text-neutral-500 mt-0.5">
                Account
              </p>
            </div>
          </DropdownMenu.Trigger>

          <DropdownMenu.Portal>
            <DropdownMenu.Content
              className="min-w-[240px] bg-white rounded-xl p-2 shadow-xl border border-neutral-200 animate-in fade-in slide-in-from-top-2"
              sideOffset={8}
              align="end"
            >
              {/* User Info */}
              <div className="px-3 py-3 border-b border-neutral-100">
                <p className="text-sm font-semibold text-neutral-900">
                  User
                </p>
                <p className="text-xs text-neutral-600 mt-0.5">
                  user@example.com
                </p>
              </div>

              {/* Menu Items */}
              <div className="py-1">
                <DropdownMenu.Item
                  className="flex items-center gap-3 px-3 py-2 text-sm text-neutral-700 rounded-lg hover:bg-neutral-100 cursor-pointer outline-none transition-colors"
                  onSelect={() => {}}
                >
                  <User className="h-4 w-4 text-neutral-500" />
                  Profile
                </DropdownMenu.Item>

                <DropdownMenu.Item
                  className="flex items-center gap-3 px-3 py-2 text-sm text-neutral-700 rounded-lg hover:bg-neutral-100 cursor-pointer outline-none transition-colors"
                  onSelect={() => {}}
                >
                  <Settings className="h-4 w-4 text-neutral-500" />
                  Settings
                </DropdownMenu.Item>

                <DropdownMenu.Item
                  className="flex items-center gap-3 px-3 py-2 text-sm text-neutral-700 rounded-lg hover:bg-neutral-100 cursor-pointer outline-none transition-colors"
                  onSelect={() => {}}
                >
                  <HelpCircle className="h-4 w-4 text-neutral-500" />
                  Help & Support
                </DropdownMenu.Item>
              </div>

              {/* Divider */}
              <div className="h-px bg-neutral-100 my-1"></div>

              {/* Sign Out */}
              <DropdownMenu.Item
                onClick={() => logout()}
                className="flex items-center gap-3 px-3 py-2 text-sm text-error-600 rounded-lg hover:bg-error-50 cursor-pointer outline-none transition-colors"
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </DropdownMenu.Item>
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>
      </div>
    </header>
  );
}

// Utility function to generate breadcrumbs from pathname
function generateBreadcrumbs(pathname: string) {
  const paths = pathname.split("/").filter(Boolean);
  const breadcrumbs = [{ label: "Home", href: "/dashboard" }];

  let currentPath = "";
  paths.forEach((path) => {
    currentPath += `/${path}`;
    const label = path
      .split("-")
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(" ");
    breadcrumbs.push({ label, href: currentPath });
  });

  return breadcrumbs;
}

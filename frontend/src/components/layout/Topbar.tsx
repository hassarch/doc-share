"use client";

import { LogOut, User } from "lucide-react";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useAuth } from "@/context/AuthContext";

export function Topbar() {
  const { logout } = useAuth();

  return (
    <header className="h-16 border-b border-hairline bg-paper flex items-center justify-between px-6">
      <div className="flex-1">
        {/* Search bar will go here in Phase 4 */}
      </div>

      <div className="flex items-center gap-4">
        <DropdownMenu.Root>
          <DropdownMenu.Trigger className="flex items-center gap-2 rounded-lg px-3 py-2 hover:bg-graphite-soft/10 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal">
            <div className="h-8 w-8 rounded-full bg-teal/10 flex items-center justify-center">
              <User className="h-4 w-4 text-teal" />
            </div>
          </DropdownMenu.Trigger>

          <DropdownMenu.Portal>
            <DropdownMenu.Content
              className="min-w-[200px] bg-paper rounded-lg p-1 shadow-xl border border-hairline"
              sideOffset={5}
            >
              <DropdownMenu.Item
                onClick={() => logout()}
                className="flex items-center gap-2 px-3 py-2 text-sm text-graphite rounded-lg hover:bg-graphite-soft/10 cursor-pointer outline-none"
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

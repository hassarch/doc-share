import { forwardRef, InputHTMLAttributes } from "react";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

export interface CheckboxProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  label?: string;
  description?: string;
}

const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ className, label, description, id, ...props }, ref) => {
    const checkboxId = id || `checkbox-${Math.random().toString(36).substr(2, 9)}`;

    return (
      <div className="flex items-start gap-3">
        <div className="flex items-center h-5">
          <div className="relative">
            <input
              ref={ref}
              type="checkbox"
              id={checkboxId}
              className={cn(
                "peer h-5 w-5 appearance-none rounded border-2 border-neutral-300",
                "bg-white transition-all duration-200 cursor-pointer",
                "checked:bg-primary-600 checked:border-primary-600",
                "focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2",
                "disabled:opacity-50 disabled:cursor-not-allowed",
                className
              )}
              {...props}
            />
            <Check className="absolute inset-0 h-5 w-5 text-white p-0.5 pointer-events-none opacity-0 peer-checked:opacity-100 transition-opacity duration-200" />
          </div>
        </div>

        {(label || description) && (
          <div className="flex-1">
            {label && (
              <label
                htmlFor={checkboxId}
                className="text-sm font-medium text-neutral-900 cursor-pointer select-none"
              >
                {label}
              </label>
            )}
            {description && (
              <p className="text-sm text-neutral-600 mt-0.5">{description}</p>
            )}
          </div>
        )}
      </div>
    );
  }
);

Checkbox.displayName = "Checkbox";

export { Checkbox };

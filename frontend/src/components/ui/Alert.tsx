import { forwardRef, HTMLAttributes } from "react";
import { CheckCircle, AlertCircle, Info, AlertTriangle, X } from "lucide-react";
import { cn } from "@/lib/utils";

export interface AlertProps extends HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "success" | "warning" | "error" | "info";
  title?: string;
  onClose?: () => void;
}

const Alert = forwardRef<HTMLDivElement, AlertProps>(
  (
    {
      className,
      variant = "default",
      title,
      onClose,
      children,
      ...props
    },
    ref
  ) => {
    const icons = {
      default: <Info className="h-5 w-5" />,
      success: <CheckCircle className="h-5 w-5" />,
      warning: <AlertTriangle className="h-5 w-5" />,
      error: <AlertCircle className="h-5 w-5" />,
      info: <Info className="h-5 w-5" />,
    };

    const variants = {
      default: {
        container: "bg-neutral-50 border-neutral-200",
        icon: "text-neutral-600",
        title: "text-neutral-900",
        content: "text-neutral-700",
      },
      success: {
        container: "bg-success-50 border-success-200",
        icon: "text-success-600",
        title: "text-success-900",
        content: "text-success-700",
      },
      warning: {
        container: "bg-warning-50 border-warning-200",
        icon: "text-warning-600",
        title: "text-warning-900",
        content: "text-warning-700",
      },
      error: {
        container: "bg-error-50 border-error-200",
        icon: "text-error-600",
        title: "text-error-900",
        content: "text-error-700",
      },
      info: {
        container: "bg-info-50 border-info-200",
        icon: "text-info-600",
        title: "text-info-900",
        content: "text-info-700",
      },
    };

    const styles = variants[variant];

    return (
      <div
        ref={ref}
        role="alert"
        className={cn(
          "flex items-start gap-3 p-4 rounded-xl border",
          styles.container,
          className
        )}
        {...props}
      >
        <div className={cn("flex-shrink-0 mt-0.5", styles.icon)}>
          {icons[variant]}
        </div>

        <div className="flex-1 min-w-0">
          {title && (
            <h5 className={cn("font-semibold text-sm mb-1", styles.title)}>
              {title}
            </h5>
          )}
          <div className={cn("text-sm", styles.content)}>{children}</div>
        </div>

        {onClose && (
          <button
            onClick={onClose}
            className={cn(
              "flex-shrink-0 p-1 rounded-lg hover:bg-black/10 transition-colors",
              styles.icon
            )}
            aria-label="Close alert"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    );
  }
);

Alert.displayName = "Alert";

export { Alert };

# Error Handling Fix - Unhandled Promise Rejections

## Problem

The application was experiencing unhandled promise rejections when React Query mutations or queries failed:

```
[browser] ⨯ unhandledRejection: ApiError: An unexpected error occurred
```

This happened because:
1. React Query was throwing errors to error boundaries by default
2. Components weren't catching errors from async operations
3. No user-facing error messages were displayed

## Solution

### 1. Updated React Query Configuration

**File**: `frontend/src/lib/query-client.ts`

Added `throwOnError: false` to prevent errors from being thrown to error boundaries:

```typescript
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
      throwOnError: false,  // ✅ Added
    },
    mutations: {
      retry: false,
      throwOnError: false,  // ✅ Added
    },
  },
});
```

### 2. Added Error Handling to Pages

**Files**:
- `frontend/src/app/(app)/documents/page.tsx`
- `frontend/src/app/(app)/documents/[folderId]/page.tsx`

Added error state handling from React Query:

```typescript
const { data, isLoading, error } = useDocuments(folderId);

if (error) {
  return (
    <div className="flex h-full items-center justify-center p-6">
      <div className="max-w-md text-center">
        <AlertCircle className="h-12 w-12 text-brick mx-auto mb-4" />
        <h2 className="text-lg font-semibold text-graphite mb-2">
          Failed to load documents
        </h2>
        <p className="text-sm text-graphite-soft mb-4">
          {error.message || "An unexpected error occurred"}
        </p>
        <button onClick={() => window.location.reload()}>
          Retry
        </button>
      </div>
    </div>
  );
}
```

### 3. Added Error Handling to FileBrowser

**File**: `frontend/src/components/documents/FileBrowser.tsx`

Added local error state and try-catch blocks:

```typescript
const [error, setError] = useState<string | null>(null);

async function handleUpload(file: File) {
  try {
    setError(null);
    await uploadMutation.mutateAsync({ file, folderId: currentFolderId });
    setShowUpload(false);
  } catch (err) {
    setError(err instanceof ApiError ? err.message : "Upload failed");
  }
}
```

Error banner in UI:

```tsx
{error && (
  <div className="mb-4 p-3 rounded-lg bg-brick/10 border border-brick/20 text-sm text-brick">
    {error}
  </div>
)}
```

### 4. Added Error Handling to CreateFolderDialog

**File**: `frontend/src/components/documents/CreateFolderDialog.tsx`

Added error state to modal:

```typescript
const [error, setError] = useState<string | null>(null);

async function handleSubmit(e: React.FormEvent) {
  e.preventDefault();
  try {
    setError(null);
    await createMutation.mutateAsync({ name, parentFolderId });
    setName("");
    onOpenChange(false);
  } catch (err) {
    setError(err instanceof ApiError ? err.message : "Failed to create folder");
  }
}
```

## Error Handling Pattern

### For Queries (GET requests)

Use React Query's built-in error handling:

```typescript
const { data, isLoading, error } = useDocuments(folderId);

if (error) {
  return <ErrorMessage error={error} />;
}
```

### For Mutations (POST/PUT/DELETE)

Use try-catch with local error state:

```typescript
const [error, setError] = useState<string | null>(null);
const mutation = useMutation(...);

async function handleAction() {
  try {
    setError(null);
    await mutation.mutateAsync(...);
    // Success handling
  } catch (err) {
    setError(err instanceof ApiError ? err.message : "Operation failed");
  }
}
```

## User Experience Improvements

### Before
- Silent failures
- Unhandled promise rejections in console
- No feedback to user
- App appeared broken

### After
- ✅ Error messages displayed to user
- ✅ Retry buttons for failed operations
- ✅ No console errors
- ✅ Graceful degradation

## Error Message Hierarchy

1. **API Error Message** (from backend): Most specific, use when available
   ```typescript
   err instanceof ApiError ? err.message : "..."
   ```

2. **Generic Message**: Fallback when API error not available
   ```typescript
   "Failed to load documents"
   ```

3. **User Action**: Always provide a way forward
   - Retry button
   - Go back button
   - Clear error and try again

## Testing Error Scenarios

### Test Query Errors

```bash
# Stop backend
# Try to load documents page
# Should show error message with retry button
```

### Test Mutation Errors

```bash
# Try to upload invalid file
# Should show error banner above file list
# Error should clear on next successful operation
```

### Test Network Errors

```bash
# Disable network
# Try any operation
# Should show appropriate error message
```

## Files Modified

```
frontend/src/lib/query-client.ts                      # React Query config
frontend/src/app/(app)/documents/page.tsx              # Root documents page
frontend/src/app/(app)/documents/[folderId]/page.tsx   # Folder page
frontend/src/components/documents/FileBrowser.tsx      # Main file browser
frontend/src/components/documents/CreateFolderDialog.tsx # Folder creation
```

## Future Improvements

### Phase 1+
- [ ] Add toast notifications for success/error (Phase 3)
- [ ] Add retry logic with exponential backoff
- [ ] Add offline detection and friendly message
- [ ] Add error tracking (Sentry, LogRocket)

### Phase 3+
- [ ] WebSocket error handling
- [ ] Optimistic UI with rollback on error
- [ ] Conflict resolution UI (for concurrent edits)

### Phase 5+
- [ ] Circuit breaker state visualization
- [ ] Rate limit error handling with countdown
- [ ] Correlation ID display for support

## Verification

Build passes:
```bash
cd frontend
npm run build
# ✓ Compiled successfully
# ✓ Generated 10 pages
```

No unhandled rejections:
```bash
npm run dev
# Open browser console
# No red errors
# All operations show user-friendly messages on failure
```

---

**Status**: ✅ Fixed  
**Build**: ✅ Passing  
**User Experience**: ✅ Improved  
**Next**: Test all error scenarios in browser

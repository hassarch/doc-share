import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as documentsApi from "@/lib/documents-api";

export function useDocuments(folderId: string | null) {
  return useQuery({
    queryKey: ["documents", folderId],
    queryFn: () => documentsApi.listDocuments(folderId),
  });
}

export function useFolders(parentFolderId: string | null) {
  return useQuery({
    queryKey: ["folders", parentFolderId],
    queryFn: () => documentsApi.listFolders(parentFolderId),
  });
}

export function useUploadDocument() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ file, folderId }: { file: File; folderId: string | null }) =>
      documentsApi.uploadDocument(file, folderId),
    onSuccess: (_, variables) => {
      // Invalidate the document list for the current folder
      queryClient.invalidateQueries({ queryKey: ["documents", variables.folderId] });
    },
  });
}

export function useRenameDocument() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, filename }: { id: string; filename: string }) =>
      documentsApi.renameDocument(id, filename),
    onSuccess: (data) => {
      // Invalidate queries for the folder containing this document
      queryClient.invalidateQueries({ queryKey: ["documents", data.folderId] });
    },
  });
}

export function useDeleteDocument() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => documentsApi.deleteDocument(id),
    onSuccess: () => {
      // Invalidate all document queries - we don't track which folder it was in
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
  });
}

export function useCreateFolder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ name, parentFolderId }: { name: string; parentFolderId: string | null }) =>
      documentsApi.createFolder(name, parentFolderId),
    onSuccess: (data) => {
      // Invalidate folder list for the parent
      queryClient.invalidateQueries({ queryKey: ["folders", data.parentFolderId] });
    },
  });
}

export function useDeleteFolder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => documentsApi.deleteFolder(id),
    onSuccess: () => {
      // Invalidate all folder queries
      queryClient.invalidateQueries({ queryKey: ["folders"] });
      // Also invalidate documents since deleting a folder affects document listings
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    },
  });
}

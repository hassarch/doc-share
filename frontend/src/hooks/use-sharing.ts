import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as sharingApi from "@/lib/sharing-api";

export function useShares(documentId: string) {
  return useQuery({
    queryKey: ["shares", documentId],
    queryFn: () => sharingApi.listShares(documentId),
  });
}

export function useShareDocument() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      documentId,
      email,
      role,
    }: {
      documentId: string;
      email: string;
      role: sharingApi.ShareRole;
    }) => sharingApi.shareDocument(documentId, email, role),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["shares", variables.documentId] });
    },
  });
}

export function useRevokeShare() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (shareId: string) => sharingApi.revokeShare(shareId),
    onSuccess: () => {
      // Invalidate all share queries since we don't track which document it belonged to
      queryClient.invalidateQueries({ queryKey: ["shares"] });
    },
  });
}

export function useShareLinks(documentId: string) {
  return useQuery({
    queryKey: ["shareLinks", documentId],
    queryFn: () => sharingApi.listShareLinks(documentId),
  });
}

export function useCreateShareLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: sharingApi.ShareLinkRequest) =>
      sharingApi.createShareLink(request),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["shareLinks", variables.documentId] });
    },
  });
}

export function useDeleteShareLink() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (linkId: string) => sharingApi.deleteShareLink(linkId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["shareLinks"] });
    },
  });
}

# Namespace — equivalent to k8s/base/namespace.yaml
resource "kubernetes_namespace_v1" "workhub" {
  count = var.manage_namespace ? 1 : 0

  metadata {
    name = var.namespace_name
    labels = merge(local.common_labels, {
      "app.kubernetes.io/name" = var.platform_name
      "workhub.io/tier"          = "platform"
    })
    annotations = {
      "pod-security.kubernetes.io/enforce"         = "restricted"
      "pod-security.kubernetes.io/enforce-version" = "latest"
    }
  }
}

# Resolved namespace for all resources (created or pre-existing)
locals {
  target_namespace = var.manage_namespace ? kubernetes_namespace_v1.workhub[0].metadata[0].name : var.namespace_name
}

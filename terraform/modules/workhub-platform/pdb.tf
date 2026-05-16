# PodDisruptionBudget — k8s/base/poddisruptionbudget.yaml
resource "kubernetes_pod_disruption_budget_v1" "api" {
  count = var.enable_pod_disruption_budget ? 1 : 0

  metadata {
    name      = local.deployment_name
    namespace = local.target_namespace
    labels    = local.app_labels
  }

  spec {
    min_available = var.pdb_min_available
    selector {
      match_labels = local.selector_labels
    }
  }
}

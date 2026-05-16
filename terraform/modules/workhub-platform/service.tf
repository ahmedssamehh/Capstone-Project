# Service — k8s/base/service.yaml (ClusterIP :80 -> pod :8080)
resource "kubernetes_service_v1" "api" {
  metadata {
    name      = local.service_name
    namespace = local.target_namespace
    labels    = local.app_labels
  }

  spec {
    type     = "ClusterIP"
    selector = local.selector_labels

    port {
      name         = "http"
      port         = var.service_port
      target_port  = "http"
      protocol     = "TCP"
      app_protocol = "http"
    }
  }
}

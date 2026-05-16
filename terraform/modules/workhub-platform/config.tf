# ServiceAccount — k8s/base/serviceaccount.yaml
resource "kubernetes_service_account_v1" "api" {
  count = var.enable_service_account ? 1 : 0

  metadata {
    name      = local.sa_name
    namespace = local.target_namespace
    labels    = local.app_labels
  }
  automount_service_account_token = false
}

# ConfigMap — k8s/base/configmap.yaml (non-sensitive only)
resource "kubernetes_config_map_v1" "api" {
  metadata {
    name      = local.config_map_name
    namespace = local.target_namespace
    labels = merge(local.app_labels, {
      "workhub.io/config-type" = "application"
    })
  }

  data = local.config_map_data
}

# Secret — k8s/base/secret.example.yaml / overlays secretGenerator
resource "kubernetes_secret_v1" "api" {
  metadata {
    name      = local.secret_name
    namespace = local.target_namespace
    labels = merge(local.app_labels, {
      "workhub.io/secret-type" = "credentials"
    })
  }

  type = "Opaque"

  data = {
    DATABASE_USERNAME = base64encode(var.database_username)
    DATABASE_PASSWORD = base64encode(var.database_password)
    JWT_SECRET        = base64encode(var.jwt_secret)
    RABBITMQ_USERNAME = base64encode(var.rabbitmq_username)
    RABBITMQ_PASSWORD = base64encode(var.rabbitmq_password)
  }

}

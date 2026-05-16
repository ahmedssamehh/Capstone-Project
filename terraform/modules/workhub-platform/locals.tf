# -----------------------------------------------------------------------------
# Naming & labels — aligned with k8s/base and k8s/overlays (Kustomize)
# -----------------------------------------------------------------------------
locals {
  # Canonical names from k8s/base/*.yaml and k8s/overlays secretGenerator
  k8s_manifest_names = {
    namespace   = "workhub"
    config_map  = "workhub-api-config"
    secret      = "workhub-api-secrets"
    deployment  = "workhub-api"
    service     = "workhub-api"
    sa          = "workhub-api"
    pdb         = "workhub-api"
  }

  config_map_name = var.align_k8s_manifests ? local.k8s_manifest_names.config_map : "${var.app_name}-config"
  secret_name     = var.align_k8s_manifests ? local.k8s_manifest_names.secret : "${var.app_name}-secrets"
  sa_name         = var.align_k8s_manifests ? local.k8s_manifest_names.sa : var.app_name
  deployment_name = var.align_k8s_manifests ? local.k8s_manifest_names.deployment : var.app_name
  service_name    = var.align_k8s_manifests ? local.k8s_manifest_names.service : var.app_name

  common_labels = {
    "app.kubernetes.io/managed-by" = var.managed_by
    "app.kubernetes.io/part-of"      = var.platform_name
    "workhub.io/environment"         = var.environment
    "workhub.io/tenant-model"        = "application-scoped"
  }

  app_labels = merge(local.common_labels, {
    "app.kubernetes.io/name"       = var.app_name
    "app.kubernetes.io/component" = "api"
    "app.kubernetes.io/version"  = var.app_version
  })

  selector_labels = {
    "app.kubernetes.io/name" = var.app_name
  }

  image = "${var.image_repository}:${var.image_tag}"

  # In-cluster DNS defaults (matches k8s/base/configmap.yaml)
  postgres_host_default = "postgres.${var.namespace_name}.svc.cluster.local"
  rabbitmq_host_default = "rabbitmq.${var.namespace_name}.svc.cluster.local"

  postgres_host = coalesce(var.database_host, local.postgres_host_default)
  rabbitmq_host = coalesce(var.rabbitmq_host, local.rabbitmq_host_default)

  database_url = coalesce(
    var.database_url,
    "jdbc:postgresql://${local.postgres_host}:${var.database_port}/${var.database_name}"
  )

  # Actuator probe paths (Spring Boot 3 — same as k8s/base/deployment.yaml)
  probe_paths = {
    liveness  = "/actuator/health/liveness"
    readiness = "/actuator/health/readiness"
  }

  config_map_data = merge(
    {
      SPRING_PROFILES_ACTIVE = var.spring_profiles_active
      DATABASE_HOST          = local.postgres_host
      DATABASE_PORT          = var.database_port
      DATABASE_NAME          = var.database_name
      DATABASE_URL           = local.database_url
      RABBITMQ_HOST          = local.rabbitmq_host
      RABBITMQ_PORT          = var.rabbitmq_port
      RABBITMQ_VHOST         = var.rabbitmq_vhost
      WORKHUB_RABBIT_EXCHANGE    = var.workhub_rabbit_exchange
      WORKHUB_RABBIT_QUEUE       = var.workhub_rabbit_queue
      WORKHUB_RABBIT_ROUTING_KEY = var.workhub_rabbit_routing_key
      JAVA_OPTS                  = var.java_opts
      LOGGING_LEVEL_COM_WORKHUB  = var.logging_level_com_workhub
      LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY = var.logging_level_security
    },
    var.extra_config_map_data
  )

  secret_keys = toset([
    "DATABASE_USERNAME",
    "DATABASE_PASSWORD",
    "JWT_SECRET",
    "RABBITMQ_USERNAME",
    "RABBITMQ_PASSWORD",
  ])
}

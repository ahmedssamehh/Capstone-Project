# =============================================================================
# WorkHub SaaS API — Terraform root module
#
# Provisions Kubernetes resources aligned with k8s/base manifests:
#   namespace, ServiceAccount, ConfigMap, Secret, Deployment, Service, PDB
#
# Usage:
#   terraform init && terraform apply -var-file=terraform.tfvars
#
# Multi-environment:
#   cd environments/production && terraform init && terraform apply
# =============================================================================

module "workhub_platform" {
  source = "./modules/workhub-platform"

  environment          = var.environment
  namespace_name       = var.namespace_name
  manage_namespace     = var.manage_namespace
  align_k8s_manifests  = var.align_k8s_manifests
  app_name             = var.app_name
  app_version          = var.app_version
  platform_name        = var.platform_name

  image_repository  = var.image_repository
  image_tag         = var.image_tag
  image_pull_policy = var.image_pull_policy
  replicas          = var.replicas
  service_port      = var.service_port
  container_port    = var.container_port

  spring_profiles_active = var.spring_profiles_active
  database_url           = var.database_url
  database_host          = var.database_host
  database_port          = var.database_port
  database_name          = var.database_name
  rabbitmq_host          = var.rabbitmq_host
  rabbitmq_port          = var.rabbitmq_port
  rabbitmq_vhost         = var.rabbitmq_vhost

  workhub_rabbit_exchange    = var.workhub_rabbit_exchange
  workhub_rabbit_queue       = var.workhub_rabbit_queue
  workhub_rabbit_routing_key = var.workhub_rabbit_routing_key
  java_opts                  = var.java_opts
  extra_config_map_data      = var.extra_config_map_data

  database_username = var.database_username
  database_password = var.database_password
  jwt_secret        = var.jwt_secret
  rabbitmq_username = var.rabbitmq_username
  rabbitmq_password = var.rabbitmq_password

  resource_requests = var.resource_requests
  resource_limits   = var.resource_limits
  probes            = var.probes
  min_ready_seconds = var.min_ready_seconds

  pdb_min_available            = var.pdb_min_available
  enable_pod_disruption_budget = var.enable_pod_disruption_budget
  enable_service_account       = var.enable_service_account
}

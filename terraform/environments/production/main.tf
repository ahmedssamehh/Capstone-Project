module "workhub_platform" {
  source = "../../modules/workhub-platform"

  environment         = "production"
  namespace_name      = "workhub"
  manage_namespace    = true
  align_k8s_manifests = true

  image_repository  = var.image_repository
  image_tag         = var.image_tag
  image_pull_policy = "Always"
  replicas          = 3

  spring_profiles_active = "prod"
  database_url           = var.database_url
  database_host          = var.database_host
  rabbitmq_host          = var.rabbitmq_host

  database_username = var.database_username
  database_password = var.database_password
  jwt_secret        = var.jwt_secret
  rabbitmq_username = var.rabbitmq_username
  rabbitmq_password = var.rabbitmq_password

  resource_requests = { cpu = "500m", memory = "768Mi" }
  resource_limits   = { cpu = "2", memory = "1536Mi" }

  min_ready_seconds = 30
  pdb_min_available = 2

  probes = {
    startup_failure_threshold   = 60
    startup_period_seconds    = 5
    liveness_period_seconds   = 30
    liveness_failure_threshold = 3
    readiness_period_seconds  = 10
    readiness_failure_threshold = 6
    readiness_success_threshold = 2
    timeout_seconds           = 5
  }
}

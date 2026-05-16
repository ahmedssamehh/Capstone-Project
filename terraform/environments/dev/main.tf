module "workhub_platform" {
  source = "../../modules/workhub-platform"

  environment         = "dev"
  namespace_name      = var.namespace_name
  manage_namespace    = true
  align_k8s_manifests = true

  image_repository = var.image_repository
  image_tag        = var.image_tag
  replicas         = 2

  spring_profiles_active = "docker"
  database_url           = var.database_url
  database_host          = var.database_host
  rabbitmq_host          = var.rabbitmq_host

  database_username = var.database_username
  database_password = var.database_password
  jwt_secret        = var.jwt_secret
  rabbitmq_username = var.rabbitmq_username
  rabbitmq_password = var.rabbitmq_password

  resource_requests = { cpu = "250m", memory = "512Mi" }
  resource_limits   = { cpu = "1", memory = "1Gi" }
  pdb_min_available = 1
}

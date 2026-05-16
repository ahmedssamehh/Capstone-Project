# =============================================================================
# Root variables — passed to modules/workhub-platform
# =============================================================================

# --- Cluster ------------------------------------------------------------------
variable "kubeconfig_path" {
  type        = string
  default     = ""
  description = "Kubeconfig path. Empty = provider default."
}

variable "kubeconfig_context" {
  type        = string
  default     = ""
  description = "Kubeconfig context. Empty = current context."
}

# --- Platform -----------------------------------------------------------------
variable "environment" {
  type        = string
  description = "dev | staging | production"

  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "environment must be dev, staging, or production."
  }
}

variable "namespace_name" {
  type    = string
  default = "workhub"
}

variable "manage_namespace" {
  type        = bool
  default     = true
  description = "Terraform creates namespace. false if k8s/base/namespace.yaml already applied."
}

variable "align_k8s_manifests" {
  type        = bool
  default     = true
  description = "Match resource names in k8s/base and k8s/overlays."
}

variable "app_name" {
  type    = string
  default = "workhub-api"
}

variable "app_version" {
  type    = string
  default = "0.0.1"
}

variable "platform_name" {
  type    = string
  default = "workhub-platform"
}

# --- Image & scale ------------------------------------------------------------
variable "image_repository" {
  type        = string
  description = "Container registry/repository without tag."
}

variable "image_tag" {
  type    = string
  default = "0.0.1-SNAPSHOT"
}

variable "image_pull_policy" {
  type    = string
  default = "IfNotPresent"
}

variable "replicas" {
  type    = number
  default = 2

  validation {
    condition     = var.replicas >= 1 && var.replicas <= 50
    error_message = "replicas must be between 1 and 50."
  }
}

variable "service_port" {
  type    = number
  default = 80
}

variable "container_port" {
  type    = number
  default = 8080
}

# --- ConfigMap (non-sensitive) — defaults match k8s/base/configmap.yaml -------
variable "spring_profiles_active" {
  type    = string
  default = "prod"
}

variable "database_url" {
  type        = string
  default     = null
  nullable    = true
  description = "Optional JDBC URL. Auto-built from host/port/db when null."
}

variable "database_host" {
  type     = string
  default  = null
  nullable = true
}

variable "database_port" {
  type    = string
  default = "5432"
}

variable "database_name" {
  type    = string
  default = "workhub"
}

variable "rabbitmq_host" {
  type     = string
  default  = null
  nullable = true
}

variable "rabbitmq_port" {
  type    = string
  default = "5672"
}

variable "rabbitmq_vhost" {
  type    = string
  default = "/"
}

variable "workhub_rabbit_exchange" {
  type    = string
  default = "workhub.jobs.direct"
}

variable "workhub_rabbit_queue" {
  type    = string
  default = "workhub.jobs.queue"
}

variable "workhub_rabbit_routing_key" {
  type    = string
  default = "workhub.jobs.process"
}

variable "java_opts" {
  type    = string
  default = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom"
}

variable "extra_config_map_data" {
  type    = map(string)
  default = {}
}

# --- Secret (sensitive) -------------------------------------------------------
variable "database_username" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.database_username) > 0
    error_message = "Set TF_VAR_database_username or use secrets.tfvars."
  }
}

variable "database_password" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.database_password) > 0
    error_message = "Set TF_VAR_database_password or use secrets.tfvars."
  }
}

variable "jwt_secret" {
  type      = string
  sensitive = true

  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "jwt_secret must be at least 32 characters."
  }
}

variable "rabbitmq_username" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.rabbitmq_username) > 0
    error_message = "Set TF_VAR_rabbitmq_username or use secrets.tfvars."
  }
}

variable "rabbitmq_password" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.rabbitmq_password) > 0
    error_message = "Set TF_VAR_rabbitmq_password or use secrets.tfvars."
  }
}

# --- Resources & probes -------------------------------------------------------
variable "resource_requests" {
  type = object({ cpu = string, memory = string })
  default = { cpu = "250m", memory = "512Mi" }
}

variable "resource_limits" {
  type = object({ cpu = string, memory = string })
  default = { cpu = "1", memory = "1Gi" }
}

variable "probes" {
  type = object({
    startup_failure_threshold   = number
    startup_period_seconds    = number
    liveness_period_seconds   = number
    liveness_failure_threshold = number
    readiness_period_seconds  = number
    readiness_failure_threshold = number
    readiness_success_threshold = number
    timeout_seconds           = number
  })
  default = {
    startup_failure_threshold   = 48
    startup_period_seconds    = 5
    liveness_period_seconds   = 30
    liveness_failure_threshold = 3
    readiness_period_seconds  = 10
    readiness_failure_threshold = 6
    readiness_success_threshold = 1
    timeout_seconds           = 5
  }
}

variable "min_ready_seconds" {
  type    = number
  default = 15
}

variable "pdb_min_available" {
  type    = number
  default = 1

  validation {
    condition     = var.pdb_min_available >= 1 && var.pdb_min_available <= var.replicas
    error_message = "pdb_min_available must be between 1 and replicas (inclusive)."
  }
}

variable "enable_pod_disruption_budget" {
  type    = bool
  default = true
}

variable "enable_service_account" {
  type    = bool
  default = true
}

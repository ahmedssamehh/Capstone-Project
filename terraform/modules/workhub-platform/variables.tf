# =============================================================================
# workhub-platform module — variables
# Aligned with k8s/base/configmap.yaml, secret.example.yaml, deployment.yaml
# =============================================================================

variable "environment" {
  type = string
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
  description = "Create Namespace resource. Set false if namespace already exists (e.g. from k8s/base/namespace.yaml)."
  type        = bool
  default     = true
}

variable "align_k8s_manifests" {
  description = "Use resource names from k8s/ manifests (workhub-api-config, workhub-api-secrets, etc.)."
  type        = bool
  default     = true
}

variable "managed_by" {
  description = "app.kubernetes.io/managed-by label value."
  type        = string
  default     = "terraform"
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

variable "image_repository" { type = string }
variable "image_tag" {
  type    = string
  default = "0.0.1-SNAPSHOT"
}

variable "image_pull_policy" {
  type    = string
  default = "IfNotPresent"
  validation {
    condition     = contains(["Always", "IfNotPresent", "Never"], var.image_pull_policy)
    error_message = "image_pull_policy must be Always, IfNotPresent, or Never."
  }
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
  validation {
    condition     = var.service_port >= 1 && var.service_port <= 65535
    error_message = "service_port must be a valid TCP port."
  }
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "spring_profiles_active" {
  type    = string
  default = "prod"
}

variable "database_url" {
  description = "JDBC URL without credentials. Empty = auto-build from host/port/name."
  type        = string
  default     = null
  nullable    = true
}

variable "database_host" {
  type    = string
  default = null
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

variable "logging_level_com_workhub" {
  type    = string
  default = "INFO"
}

variable "logging_level_security" {
  type    = string
  default = "WARN"
}

variable "extra_config_map_data" {
  description = "Additional non-sensitive keys merged into ConfigMap."
  type        = map(string)
  default     = {}
}

variable "database_username" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.database_username) > 0
    error_message = "database_username must be set (TF_VAR_database_username or secrets.tfvars)."
  }
}

variable "database_password" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.database_password) > 0
    error_message = "database_password must be set (TF_VAR_database_password or secrets.tfvars)."
  }
}

variable "jwt_secret" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "jwt_secret must be at least 32 characters for HS256."
  }
}

variable "rabbitmq_username" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.rabbitmq_username) > 0
    error_message = "rabbitmq_username must be set."
  }
}

variable "rabbitmq_password" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.rabbitmq_password) > 0
    error_message = "rabbitmq_password must be set."
  }
}

variable "resource_requests" {
  type = object({ cpu = string, memory = string })
  default = { cpu = "250m", memory = "512Mi" }
}

variable "resource_limits" {
  type = object({ cpu = string, memory = string })
  default = { cpu = "1", memory = "1Gi" }
}

variable "probes" {
  description = "Actuator HTTP probe tuning (matches k8s/base/deployment.yaml defaults)."
  type = object({
    startup_failure_threshold  = number
    startup_period_seconds     = number
    liveness_period_seconds    = number
    liveness_failure_threshold = number
    readiness_period_seconds   = number
    readiness_failure_threshold = number
    readiness_success_threshold = number
    timeout_seconds            = number
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

variable "progress_deadline_seconds" {
  type    = number
  default = 600
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
  description = "Create ServiceAccount (k8s/base/serviceaccount.yaml)."
  type        = bool
  default     = true
}

variable "prometheus_scrape" {
  type    = bool
  default = true
}

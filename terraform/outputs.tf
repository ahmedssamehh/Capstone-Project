output "namespace" {
  description = "WorkHub Kubernetes namespace."
  value       = module.workhub_platform.namespace
}

output "deployment_name" {
  description = "API Deployment resource name."
  value       = module.workhub_platform.deployment_name
}

output "service_name" {
  description = "ClusterIP Service name."
  value       = module.workhub_platform.service_name
}

output "service_cluster_dns" {
  description = "Internal cluster DNS for Ingress/backend routing."
  value       = module.workhub_platform.service_cluster_dns
}

output "service_port" {
  description = "Service port number."
  value       = module.workhub_platform.service_port
}

output "config_map_name" {
  description = "Non-sensitive configuration ConfigMap."
  value       = module.workhub_platform.config_map_name
}

output "secret_name" {
  description = "Credentials Secret (name only)."
  value       = module.workhub_platform.secret_name
  sensitive   = true
}

output "secret_keys" {
  description = "Managed secret keys (not values)."
  value       = module.workhub_platform.secret_keys
}

output "database_url_resolved" {
  description = "Effective JDBC URL without password."
  value       = module.workhub_platform.database_url_resolved
}

output "k8s_manifest_alignment" {
  description = "Terraform ↔ k8s/ manifest name mapping."
  value       = module.workhub_platform.k8s_manifest_alignment
}

output "app_labels" {
  description = "Labels for observability and policy selectors."
  value       = module.workhub_platform.app_labels
}

output "kubectl_commands" {
  description = "Useful post-deploy verification commands."
  value = {
    pods    = "kubectl -n ${module.workhub_platform.namespace} get pods -l app.kubernetes.io/name=${var.app_name}"
    health  = "kubectl -n ${module.workhub_platform.namespace} port-forward svc/${module.workhub_platform.service_name} 8080:${module.workhub_platform.service_port}"
    rollout = "kubectl -n ${module.workhub_platform.namespace} rollout status deployment/${module.workhub_platform.deployment_name}"
  }
}

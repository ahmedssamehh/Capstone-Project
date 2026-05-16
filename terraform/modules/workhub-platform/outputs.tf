output "namespace" {
  description = "Target namespace for WorkHub platform workloads."
  value       = local.target_namespace
}

output "deployment_name" {
  description = "Kubernetes Deployment name."
  value       = kubernetes_deployment_v1.api.metadata[0].name
}

output "deployment_uid" {
  description = "Deployment UID (for audit references)."
  value       = kubernetes_deployment_v1.api.metadata[0].uid
}

output "service_name" {
  description = "ClusterIP Service name."
  value       = kubernetes_service_v1.api.metadata[0].name
}

output "service_cluster_dns" {
  description = "In-cluster DNS FQDN for API traffic."
  value       = "${kubernetes_service_v1.api.metadata[0].name}.${local.target_namespace}.svc.cluster.local"
}

output "service_port" {
  description = "Service port exposed inside the cluster."
  value       = var.service_port
}

output "config_map_name" {
  description = "Application ConfigMap name (non-sensitive config)."
  value       = kubernetes_config_map_v1.api.metadata[0].name
}

output "config_map_data_keys" {
  description = "Keys provisioned in ConfigMap (for drift checks)."
  value       = keys(local.config_map_data)
}

output "secret_name" {
  description = "Application Secret name."
  value       = kubernetes_secret_v1.api.metadata[0].name
  sensitive   = true
}

output "secret_keys" {
  description = "Secret keys managed by Terraform (values never exported)."
  value       = sort(tolist(local.secret_keys))
}

output "service_account_name" {
  description = "Pod ServiceAccount name, if created."
  value       = var.enable_service_account ? kubernetes_service_account_v1.api[0].metadata[0].name : null
}

output "app_labels" {
  description = "Standard labels applied to all resources."
  value       = local.app_labels
}

output "k8s_manifest_alignment" {
  description = "Whether resource names match k8s/ Kustomize manifests."
  value = {
    enabled    = var.align_k8s_manifests
    names_used = {
      config_map = local.config_map_name
      secret     = local.secret_name
      deployment = local.deployment_name
      service    = local.service_name
    }
    k8s_canonical = local.k8s_manifest_names
  }
}

output "database_url_resolved" {
  description = "Effective JDBC URL (no password) passed to the application."
  value       = local.database_url
}

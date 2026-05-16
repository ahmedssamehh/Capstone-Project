output "service_cluster_dns" {
  value = module.workhub_platform.service_cluster_dns
}

output "deployment_name" {
  value = module.workhub_platform.deployment_name
}

output "kubectl_rollout" {
  value = "kubectl -n ${module.workhub_platform.namespace} rollout status deployment/${module.workhub_platform.deployment_name}"
}

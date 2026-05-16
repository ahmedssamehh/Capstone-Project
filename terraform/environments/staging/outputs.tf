output "namespace" {
  value = module.workhub_platform.namespace
}

output "service_cluster_dns" {
  value = module.workhub_platform.service_cluster_dns
}

output "k8s_manifest_alignment" {
  value = module.workhub_platform.k8s_manifest_alignment
}

provider "kubernetes" {
  # Uses kubeconfig from KUBECONFIG env or ~/.kube/config by default.
  # Override for CI with config_path / config_context / host+token variables.
  config_path    = var.kubeconfig_path != "" ? var.kubeconfig_path : null
  config_context = var.kubeconfig_context != "" ? var.kubeconfig_context : null
}

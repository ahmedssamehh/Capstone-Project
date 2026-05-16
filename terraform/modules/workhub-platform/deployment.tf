# Deployment — k8s/base/deployment.yaml (probes, security, rolling update)
resource "kubernetes_deployment_v1" "api" {
  lifecycle {
    precondition {
      condition     = !var.enable_pod_disruption_budget || var.pdb_min_available <= var.replicas
      error_message = "When PDB is enabled, pdb_min_available must not exceed replicas."
    }
  }

  metadata {
    name      = local.deployment_name
    namespace = local.target_namespace
    labels    = local.app_labels
  }

  spec {
    replicas = var.replicas

    revision_history_limit    = 5
    progress_deadline_seconds = var.progress_deadline_seconds
    min_ready_seconds         = var.min_ready_seconds

    selector {
      match_labels = local.selector_labels
    }

    strategy {
      type = "RollingUpdate"
      rolling_update {
        max_surge       = "1"
        max_unavailable = "0"
      }
    }

    template {
      metadata {
        labels = merge(local.app_labels, {
          "workhub.io/workload" = "shared-api"
        })
        annotations = var.prometheus_scrape ? {
          "prometheus.io/scrape" = "true"
          "prometheus.io/path"   = "/actuator/prometheus"
          "prometheus.io/port"   = tostring(var.container_port)
        } : {}
      }

      spec {
        service_account_name = var.enable_service_account ? kubernetes_service_account_v1.api[0].metadata[0].name : null
        automount_service_account_token  = false
        termination_grace_period_seconds = 60

        security_context {
          run_as_non_root = true
          run_as_user     = 1000
          run_as_group    = 1000
          fs_group        = 1000
          seccomp_profile { type = "RuntimeDefault" }
        }

        container {
          name              = var.app_name
          image             = local.image
          image_pull_policy = var.image_pull_policy

          port {
            name           = "http"
            container_port = var.container_port
            protocol       = "TCP"
          }

          # Non-sensitive: ConfigMap (same keys as application-prod.yml / application-docker.yml)
          env_from {
            config_map_ref {
              name = kubernetes_config_map_v1.api.metadata[0].name
            }
          }

          # Sensitive: explicit secretKeyRef (k8s/base/deployment.yaml pattern)
          dynamic "env" {
            for_each = local.secret_keys
            content {
              name = env.key
              value_from {
                secret_key_ref {
                  name = kubernetes_secret_v1.api.metadata[0].name
                  key  = env.key
                }
              }
            }
          }

          resources {
            requests = {
              cpu    = var.resource_requests.cpu
              memory = var.resource_requests.memory
            }
            limits = {
              cpu    = var.resource_limits.cpu
              memory = var.resource_limits.memory
            }
          }

          security_context {
            allow_privilege_escalation = false
            capabilities { drop = ["ALL"] }
          }

          startup_probe {
            http_get {
              path   = local.probe_paths.liveness
              port   = "http"
              scheme = "HTTP"
            }
            period_seconds    = var.probes.startup_period_seconds
            timeout_seconds     = var.probes.timeout_seconds
            failure_threshold   = var.probes.startup_failure_threshold
            success_threshold   = 1
          }

          liveness_probe {
            http_get {
              path   = local.probe_paths.liveness
              port   = "http"
              scheme = "HTTP"
            }
            period_seconds    = var.probes.liveness_period_seconds
            timeout_seconds     = var.probes.timeout_seconds
            failure_threshold   = var.probes.liveness_failure_threshold
            success_threshold   = 1
          }

          readiness_probe {
            http_get {
              path   = local.probe_paths.readiness
              port   = "http"
              scheme = "HTTP"
            }
            period_seconds    = var.probes.readiness_period_seconds
            timeout_seconds     = var.probes.timeout_seconds
            failure_threshold   = var.probes.readiness_failure_threshold
            success_threshold   = var.probes.readiness_success_threshold
          }

          lifecycle {
            pre_stop {
              exec { command = ["sh", "-c", "sleep 10"] }
            }
          }
        }

        affinity {
          pod_anti_affinity {
            preferred_during_scheduling_ignored_during_execution {
              weight = 100
              pod_affinity_term {
                topology_key = "kubernetes.io/hostname"
                label_selector {
                  match_labels = local.selector_labels
                }
              }
            }
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_config_map_v1.api,
    kubernetes_secret_v1.api,
  ]
}

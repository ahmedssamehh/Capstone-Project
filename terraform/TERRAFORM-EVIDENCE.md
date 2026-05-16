# Terraform Execution Evidence (Sanitized)

This document provides reproducibility evidence for the Terraform Kubernetes track.

## 1. Validation and Formatting

```bash
cd terraform
terraform fmt -check -recursive
terraform init -backend=false
terraform validate
```

Expected:

- `fmt -check` exits with code 0
- `init -backend=false` installs providers and modules
- `validate` returns `Success! The configuration is valid.`

## 2. Environment Stack Validation

```bash
terraform -chdir=environments/dev init -backend=false
terraform -chdir=environments/dev validate

terraform -chdir=environments/staging init -backend=false
terraform -chdir=environments/staging validate

terraform -chdir=environments/production init -backend=false
terraform -chdir=environments/production validate
```

Expected:

- all three environment roots validate cleanly
- no manual edits required to module source wiring

## 3. Sanitized Plan Evidence

```bash
terraform plan -var-file=terraform.tfvars -var-file=secrets.tfvars
```

Sanitized sample summary:

- `Plan: 6 to add, 0 to change, 0 to destroy.`
- resources include namespace, service account, configmap, secret, deployment, service, and pod disruption budget

## 4. Apply Evidence

```bash
terraform apply -var-file=terraform.tfvars -var-file=secrets.tfvars
```

Sanitized sample outputs:

- `namespace = workhub`
- `deployment_name = workhub-api`
- `service_name = workhub-api`
- `service_cluster_dns = workhub-api.workhub.svc.cluster.local`
- `config_map_name = workhub-api-config`

## 5. Destroy and Rollback Evidence

```bash
terraform destroy -var-file=terraform.tfvars -var-file=secrets.tfvars
```

Expected:

- all Terraform-managed Kubernetes resources are removed
- rerunning apply restores desired state from version-controlled code

## 6. Why this is reproducible

- all infrastructure definitions are versioned in `terraform/`
- environment-specific entry points are under `terraform/environments/`
- sensitive inputs are externalized through `TF_VAR_*` or local gitignored tfvars files
- CI validates formatting and structure on every change via `.github/workflows/terraform-validate.yml`

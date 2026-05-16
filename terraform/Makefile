# WorkHub Terraform — common operations (requires make, terraform, kubectl)
.PHONY: init plan apply destroy validate fmt verify

ENV ?= dev
ROOT_DIR := $(CURDIR)
ENV_DIR := $(ROOT_DIR)/environments/$(ENV)

init:
	terraform -chdir=$(ENV_DIR) init -upgrade

validate:
	terraform -chdir=$(ROOT_DIR) fmt -check -recursive
	terraform -chdir=$(ENV_DIR) validate

plan:
	terraform -chdir=$(ENV_DIR) plan \
		-var-file=terraform.tfvars \
		-var-file=secrets.tfvars

apply:
	terraform -chdir=$(ENV_DIR) apply \
		-var-file=terraform.tfvars \
		-var-file=secrets.tfvars

destroy:
	terraform -chdir=$(ENV_DIR) destroy \
		-var-file=terraform.tfvars \
		-var-file=secrets.tfvars

verify:
	kubectl -n workhub get pods,svc
	kubectl -n workhub rollout status deployment/workhub-api --timeout=180s

fmt:
	terraform -chdir=$(ROOT_DIR) fmt -recursive

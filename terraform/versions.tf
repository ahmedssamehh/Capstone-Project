terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.35"
    }
  }

  # Uncomment for remote state (recommended for teams):
  # backend "s3" {
  #   bucket         = "workhub-terraform-state"
  #   key            = "platform/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "workhub-terraform-locks"
  # }
}

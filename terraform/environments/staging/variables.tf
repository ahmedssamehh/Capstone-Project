variable "image_repository" { type = string }
variable "image_tag" { type = string }

variable "database_url" {
  type     = string
  default  = null
  nullable = true
}

variable "database_host" {
  type     = string
  default  = null
  nullable = true
}

variable "rabbitmq_host" {
  type     = string
  default  = null
  nullable = true
}

variable "database_username" { type = string, sensitive = true }
variable "database_password" { type = string, sensitive = true }
variable "jwt_secret" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "jwt_secret must be at least 32 characters."
  }
}
variable "rabbitmq_username" { type = string, sensitive = true }
variable "rabbitmq_password" { type = string, sensitive = true }

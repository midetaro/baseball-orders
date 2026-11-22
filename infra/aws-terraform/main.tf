terraform {
  required_version = ">= 1.8.0"

  # GitHub Actions supplies the S3 backend values at terraform init time.
  backend "s3" {}

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

variable "aws_region" {
  description = "AWS region in which resources are created."
  type        = string
  default     = "ap-northeast-1"
}

variable "aws_profile" {
  description = "Optional local AWS shared-config profile name. Leave null for the default credential chain, including GitHub Actions OIDC credentials."
  type        = string
  default     = null
  nullable    = true
}

variable "project_name" {
  description = "Name used as the prefix for AWS resources."
  type        = string
  default     = "baseball-orders"

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9-]{1,30}[a-z0-9]$", var.project_name))
    error_message = "project_name must contain 3-32 lowercase letters, digits, or hyphens."
  }
}

variable "environment" {
  description = "Deployment environment name, such as dev, staging, or prod."
  type        = string
  default     = "dev"

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9-]{0,14}[a-z0-9]$", var.environment))
    error_message = "environment must contain 2-16 lowercase letters, digits, or hyphens."
  }
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Environment = var.environment
      ManagedBy   = "Terraform"
      Project     = var.project_name
    }
  }
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

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

variable "request_queue_name" {
  description = "Simulation request queue name. The default matches the backend application."
  type        = string
  default     = "simulation-request"
}

variable "result_queue_name" {
  description = "Simulation result queue name. The default matches the backend application."
  type        = string
  default     = "simulation-result"
}

variable "message_retention_seconds" {
  description = "Retention period for request and result messages."
  type        = number
  default     = 345600

  validation {
    condition     = var.message_retention_seconds >= 60 && var.message_retention_seconds <= 1209600
    error_message = "message_retention_seconds must be between 60 and 1209600."
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

data "aws_iam_policy_document" "backend_sqs" {
  statement {
    sid       = "SendSimulationRequests"
    actions   = ["sqs:GetQueueAttributes", "sqs:GetQueueUrl", "sqs:SendMessage"]
    resources = [aws_sqs_queue.simulation_request.arn]
  }

  statement {
    sid       = "ConsumeSimulationResults"
    actions   = ["sqs:ChangeMessageVisibility", "sqs:DeleteMessage", "sqs:GetQueueAttributes", "sqs:GetQueueUrl", "sqs:ReceiveMessage"]
    resources = [aws_sqs_queue.simulation_result.arn]
  }
}

resource "aws_iam_policy" "backend_sqs" {
  name        = "${local.name_prefix}-backend-sqs"
  description = "Least-privilege SQS access for the baseball-orders backend."
  policy      = data.aws_iam_policy_document.backend_sqs.json
}

data "aws_iam_policy_document" "simulator_sqs" {
  statement {
    sid       = "ConsumeSimulationRequests"
    actions   = ["sqs:ChangeMessageVisibility", "sqs:DeleteMessage", "sqs:GetQueueAttributes", "sqs:GetQueueUrl", "sqs:ReceiveMessage"]
    resources = [aws_sqs_queue.simulation_request.arn]
  }

  statement {
    sid       = "SendSimulationResults"
    actions   = ["sqs:GetQueueAttributes", "sqs:GetQueueUrl", "sqs:SendMessage"]
    resources = [aws_sqs_queue.simulation_result.arn]
  }
}

resource "aws_iam_policy" "simulator_sqs" {
  name        = "${local.name_prefix}-simulator-sqs"
  description = "Least-privilege SQS access for the baseball-orders simulator."
  policy      = data.aws_iam_policy_document.simulator_sqs.json
}

output "backend_sqs_policy_arn" {
  value = aws_iam_policy.backend_sqs.arn
}

output "simulator_sqs_policy_arn" {
  value = aws_iam_policy.simulator_sqs.arn
}

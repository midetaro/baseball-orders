package main

// RenderTerraform returns the native Terraform configuration used to manage the
// AWS resources for baseball-orders.
func RenderTerraform() string {
	return terraformConfiguration
}

const terraformConfiguration = `terraform {
  required_version = ">= 1.8.0"

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
  region = var.aws_region

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

resource "aws_sqs_queue" "simulation_request_dlq" {
  name                       = "${var.request_queue_name}-dlq"
  message_retention_seconds  = 1209600
  sqs_managed_sse_enabled    = true
}

resource "aws_sqs_queue" "simulation_result_dlq" {
  name                       = "${var.result_queue_name}-dlq"
  message_retention_seconds  = 1209600
  sqs_managed_sse_enabled    = true
}

resource "aws_sqs_queue" "simulation_request" {
  name                       = var.request_queue_name
  message_retention_seconds  = var.message_retention_seconds
  receive_wait_time_seconds  = 20
  visibility_timeout_seconds = 60
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.simulation_request_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue" "simulation_result" {
  name                       = var.result_queue_name
  message_retention_seconds  = var.message_retention_seconds
  receive_wait_time_seconds  = 20
  visibility_timeout_seconds = 60
  sqs_managed_sse_enabled    = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.simulation_result_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "simulation_request" {
  queue_url = aws_sqs_queue.simulation_request_dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.simulation_request.arn]
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "simulation_result" {
  queue_url = aws_sqs_queue.simulation_result_dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.simulation_result.arn]
  })
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

output "simulation_request_queue_url" {
  description = "Set this value as simulation.sqs.request-queue-url for the simulator."
  value       = aws_sqs_queue.simulation_request.url
}

output "simulation_request_queue_arn" {
  value = aws_sqs_queue.simulation_request.arn
}

output "simulation_result_queue_url" {
  value = aws_sqs_queue.simulation_result.url
}

output "simulation_result_queue_arn" {
  value = aws_sqs_queue.simulation_result.arn
}

output "backend_sqs_policy_arn" {
  value = aws_iam_policy.backend_sqs.arn
}

output "simulator_sqs_policy_arn" {
  value = aws_iam_policy.simulator_sqs.arn
}
`

variable "log_retention_days" {
  description = "CloudWatch Logs retention, in days, for the backend and simulator ECS log groups."
  type        = number
  default     = 14
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.name_prefix}-backend"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_log_group" "simulator" {
  name              = "/ecs/${local.name_prefix}-simulator"
  retention_in_days = var.log_retention_days
}

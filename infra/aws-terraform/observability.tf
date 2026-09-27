variable "log_retention_days" {
  description = "CloudWatch Logs retention, in days, for the backend and simulator ECS log groups."
  type        = number
  default     = 14
}

variable "dlq_alarm_actions" {
  description = "ARNs (for example, an SNS topic) notified when a message reaches either DLQ. Empty by default; every DLQ alarm still exists and is visible in the CloudWatch console without a configured destination."
  type        = list(string)
  default     = []
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.name_prefix}-backend"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_log_group" "simulator" {
  name              = "/ecs/${local.name_prefix}-simulator"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_metric_alarm" "request_dlq_not_empty" {
  alarm_name          = "${local.name_prefix}-simulation-request-dlq-not-empty"
  alarm_description   = "A message reached the simulation-request DLQ, meaning simulation requests failed processing 5 times (maxReceiveCount)."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Maximum"
  threshold           = 0

  dimensions = {
    QueueName = aws_sqs_queue.simulation_request_dlq.name
  }

  alarm_actions = var.dlq_alarm_actions
  ok_actions    = var.dlq_alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "result_dlq_not_empty" {
  alarm_name          = "${local.name_prefix}-simulation-result-dlq-not-empty"
  alarm_description   = "A message reached the simulation-result DLQ, meaning simulation results failed delivery 5 times (maxReceiveCount)."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Maximum"
  threshold           = 0

  dimensions = {
    QueueName = aws_sqs_queue.simulation_result_dlq.name
  }

  alarm_actions = var.dlq_alarm_actions
  ok_actions    = var.dlq_alarm_actions
}

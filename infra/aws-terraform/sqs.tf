resource "aws_sqs_queue" "simulation_request_dlq" {
  name                      = "${var.request_queue_name}-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "simulation_result_dlq" {
  name                      = "${var.result_queue_name}-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
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

output "simulation_request_queue_url" {
  description = "URL of the simulation request queue."
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

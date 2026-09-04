mock_provider "aws" {}

run "sqs_configuration" {
  command = plan

  assert {
    condition     = aws_sqs_queue.simulation_request.name == "simulation-request"
    error_message = "The request queue must use the backend-compatible default name."
  }

  assert {
    condition     = aws_sqs_queue.simulation_result.name == "simulation-result"
    error_message = "The result queue must use the backend-compatible default name."
  }

  assert {
    condition = alltrue([
      aws_sqs_queue.simulation_request.sqs_managed_sse_enabled,
      aws_sqs_queue.simulation_result.sqs_managed_sse_enabled,
      aws_sqs_queue.simulation_request_dlq.sqs_managed_sse_enabled,
      aws_sqs_queue.simulation_result_dlq.sqs_managed_sse_enabled,
    ])
    error_message = "Every SQS queue must use SQS-managed server-side encryption."
  }

  assert {
    condition = alltrue([
      jsondecode(aws_sqs_queue.simulation_request.redrive_policy).maxReceiveCount == 5,
      jsondecode(aws_sqs_queue.simulation_result.redrive_policy).maxReceiveCount == 5,
    ])
    error_message = "Request and result queues must move messages to their DLQs after five receives."
  }
}

run "iam_configuration" {
  command = plan

  assert {
    condition     = aws_iam_policy.backend_sqs.name == "baseball-orders-dev-backend-sqs"
    error_message = "The backend IAM policy must use the project and environment prefix."
  }

  assert {
    condition     = aws_iam_policy.simulator_sqs.name == "baseball-orders-dev-simulator-sqs"
    error_message = "The simulator IAM policy must use the project and environment prefix."
  }
}

data "aws_iam_policy_document" "ecs_tasks_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_task_execution" {
  name               = "${local.name_prefix}-ecs-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "backend_task" {
  name               = "${local.name_prefix}-backend-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role_policy_attachment" "backend_task_sqs" {
  role       = aws_iam_role.backend_task.name
  policy_arn = aws_iam_policy.backend_sqs.arn
}

resource "aws_iam_role" "simulator_task" {
  name               = "${local.name_prefix}-simulator-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role_policy_attachment" "simulator_task_sqs" {
  role       = aws_iam_role.simulator_task.name
  policy_arn = aws_iam_policy.simulator_sqs.arn
}

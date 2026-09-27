variable "backend_image_tag" {
  description = "Backend container image tag used on the first apply. deploy.yml swaps the running image afterward; lifecycle.ignore_changes on container_definitions keeps Terraform from reverting it."
  type        = string
  default     = "latest"
}

variable "simulator_image_tag" {
  description = "Simulator container image tag used on the first apply. See backend_image_tag."
  type        = string
  default     = "latest"
}

variable "sqs_poll_fixed_delay" {
  description = "SIMULATION_SQS_POLL_FIXED_DELAY for the simulator. Must stay below the backend's 30s synchronous wait (docs/aws-deployment.md); the application-prod.yml default of 60s is unsafe here."
  type        = string
  default     = "1s"
}

resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.backend_task.arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        },
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SIMULATION_REQUEST_QUEUE_NAME", value = aws_sqs_queue.simulation_request.name },
        { name = "SIMULATION_RESULT_QUEUE_NAME", value = aws_sqs_queue.simulation_result.name },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.backend.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "backend"
        }
      }
    },
  ])

  lifecycle {
    ignore_changes = [container_definitions]
  }
}

resource "aws_ecs_task_definition" "simulator" {
  family                   = "${local.name_prefix}-simulator"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.simulator_task.arn

  container_definitions = jsonencode([
    {
      name      = "simulator"
      image     = "${aws_ecr_repository.simulator.repository_url}:${var.simulator_image_tag}"
      essential = true

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SIMULATION_REQUEST_QUEUE_NAME", value = aws_sqs_queue.simulation_request.name },
        { name = "SIMULATION_RESULT_QUEUE_NAME", value = aws_sqs_queue.simulation_result.name },
        { name = "SIMULATION_SQS_POLL_FIXED_DELAY", value = var.sqs_poll_fixed_delay },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.simulator.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "simulator"
        }
      }
    },
  ])

  lifecycle {
    ignore_changes = [container_definitions]
  }
}

resource "aws_ecs_service" "backend" {
  name            = "${local.name_prefix}-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.backend.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.https]
}

resource "aws_ecs_service" "simulator" {
  name            = "${local.name_prefix}-simulator"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.simulator.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 100
  }

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.simulator.id]
    assign_public_ip = true
  }
}

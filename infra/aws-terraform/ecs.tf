variable "backend_image_tag" {
  description = "Backend container image tag used on the first apply. push-ecr.yml tags every main build as main; lifecycle.ignore_changes on container_definitions keeps Terraform from reverting a later image swap."
  type        = string
  default     = "main"
}

variable "simulator_image_tag" {
  description = "Simulator container image tag used on the first apply. See backend_image_tag."
  type        = string
  default     = "main"
}

variable "task_memory" {
  description = "Fargate task memory in MiB for both the backend and the simulator."
  type        = string
  default     = "4096"
}

variable "jvm_options" {
  description = "JAVA_TOOL_OPTIONS for both containers. -Xmx2g caps the heap at 2 GB and leaves the rest of task_memory for metaspace, threads, and native memory."
  type        = string
  default     = "-Xmx2g"
}

variable "simulator_grpc_port" {
  description = "Container port on which the simulator serves gRPC (SIMULATION_GRPC_PORT). The backend reaches it as simulator:<port> through ECS Service Connect (SIMULATOR_GRPC_HOST/SIMULATOR_GRPC_PORT)."
  type        = number
  default     = 9090
}

resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]
}

locals {
  simulator_grpc_port_name   = "grpc"
  simulator_service_dns_name = "simulator"
}

resource "aws_service_discovery_http_namespace" "main" {
  name        = "${local.name_prefix}.internal"
  description = "ECS Service Connect namespace for the backend -> simulator gRPC call."
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.backend_task.arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = "${data.aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        },
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "JAVA_TOOL_OPTIONS", value = var.jvm_options },
        { name = "SIMULATOR_GRPC_HOST", value = local.simulator_service_dns_name },
        { name = "SIMULATOR_GRPC_PORT", value = tostring(var.simulator_grpc_port) },
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
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.simulator_task.arn

  container_definitions = jsonencode([
    {
      name      = "simulator"
      image     = "${data.aws_ecr_repository.simulator.repository_url}:${var.simulator_image_tag}"
      essential = true

      portMappings = [
        {
          name          = local.simulator_grpc_port_name
          containerPort = var.simulator_grpc_port
          protocol      = "tcp"
          appProtocol   = "grpc"
        },
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "JAVA_TOOL_OPTIONS", value = var.jvm_options },
        { name = "SIMULATION_GRPC_PORT", value = tostring(var.simulator_grpc_port) },
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

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn
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

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.main.arn

    service {
      port_name      = local.simulator_grpc_port_name
      discovery_name = local.simulator_service_dns_name

      client_alias {
        port     = var.simulator_grpc_port
        dns_name = local.simulator_service_dns_name
      }
    }
  }
}

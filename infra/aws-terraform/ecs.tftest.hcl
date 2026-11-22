mock_provider "aws" {}

variables {
  domain_name       = "orders.example.com"
  route53_zone_name = "example.com"
}

run "cluster_and_task_definitions" {
  command = apply

  override_resource {
    target = aws_iam_role.ecs_task_execution
    values = {
      arn = "arn:aws:iam::123456789012:role/baseball-orders-dev-ecs-task-execution"
    }
  }

  override_resource {
    target = aws_iam_role.backend_task
    values = {
      arn = "arn:aws:iam::123456789012:role/baseball-orders-dev-backend-task"
    }
  }

  override_resource {
    target = aws_iam_role.simulator_task
    values = {
      arn = "arn:aws:iam::123456789012:role/baseball-orders-dev-simulator-task"
    }
  }

  override_data {
    target = data.aws_availability_zones.available
    values = {
      names = ["ap-northeast-1a", "ap-northeast-1c"]
    }
  }

  override_data {
    target = data.aws_iam_policy_document.ecs_tasks_assume_role
    values = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"sts:AssumeRole\",\"Principal\":{\"Service\":\"ecs-tasks.amazonaws.com\"}}]}"
    }
  }

  override_data {
    target = data.aws_route53_zone.main
    values = {
      zone_id = "Z1234567890ABC"
      name    = "example.com"
    }
  }

  override_resource {
    target          = aws_acm_certificate.main
    override_during = plan
    values = {
      arn                 = "arn:aws:acm:ap-northeast-1:123456789012:certificate/11111111-2222-3333-4444-555555555555"
      id                  = "arn:aws:acm:ap-northeast-1:123456789012:certificate/11111111-2222-3333-4444-555555555555"
      status              = "PENDING_VALIDATION"
      not_before          = ""
      not_after           = ""
      renewal_eligibility = "INELIGIBLE"
      key_algorithm       = "RSA_2048"
      type                = "AMAZON_ISSUED"
      region              = "ap-northeast-1"
      domain_validation_options = [
        {
          domain_name           = "orders.example.com"
          resource_record_name  = "_abc123.orders.example.com."
          resource_record_type  = "CNAME"
          resource_record_value = "_xyz456.acm-validations.aws."
        },
      ]
    }
  }

  override_resource {
    target = aws_lb.main
    values = {
      arn = "arn:aws:elasticloadbalancing:ap-northeast-1:123456789012:loadbalancer/app/baseball-orders-dev-alb/1234567890abcdef"
    }
  }

  override_resource {
    target = aws_lb_target_group.backend
    values = {
      arn = "arn:aws:elasticloadbalancing:ap-northeast-1:123456789012:targetgroup/baseball-orders-dev-backend/1234567890abcdef"
    }
  }

  assert {
    condition     = aws_ecs_cluster.main.name == "baseball-orders-dev-cluster"
    error_message = "The ECS cluster must use the project and environment prefix."
  }

  assert {
    condition     = toset(aws_ecs_cluster_capacity_providers.main.capacity_providers) == toset(["FARGATE", "FARGATE_SPOT"])
    error_message = "The cluster must support both FARGATE (backend) and FARGATE_SPOT (simulator)."
  }

  assert {
    condition     = aws_ecs_task_definition.backend.cpu == "512" && aws_ecs_task_definition.backend.memory == "4096"
    error_message = "The backend task must request 0.5 vCPU / 4 GB."
  }

  assert {
    condition     = aws_ecs_task_definition.simulator.cpu == "1024" && aws_ecs_task_definition.simulator.memory == "4096"
    error_message = "The simulator task must request 1 vCPU / 4 GB."
  }

  assert {
    condition = alltrue([
      { for e in jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].environment : e.name => e.value }["JAVA_TOOL_OPTIONS"] == "-Xmx2g",
      { for e in jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].environment : e.name => e.value }["JAVA_TOOL_OPTIONS"] == "-Xmx2g",
    ])
    error_message = "Both containers must cap the JVM heap at 2 GB through JAVA_TOOL_OPTIONS."
  }

  assert {
    condition = alltrue([
      jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].image == "${data.aws_ecr_repository.backend.repository_url}:main",
      jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].image == "${data.aws_ecr_repository.simulator.repository_url}:main",
    ])
    error_message = "Both containers must pull the main-tagged image pushed by push-ecr.yml from the existing ECR repositories."
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].portMappings[0].containerPort == 8080
    error_message = "The backend container must expose port 8080, matching the ALB target group and health check."
  }

  assert {
    condition     = { for e in jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].environment : e.name => e.value }["SPRING_PROFILES_ACTIVE"] == "prod"
    error_message = "The backend container must set SPRING_PROFILES_ACTIVE=prod."
  }

  assert {
    condition     = { for e in jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].environment : e.name => e.value }["SIMULATION_GRPC_PORT"] == "9090"
    error_message = "The simulator container must listen for gRPC on 9090 (SIMULATION_GRPC_PORT)."
  }

  assert {
    condition = alltrue([
      { for e in jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].environment : e.name => e.value }["SIMULATOR_GRPC_HOST"] == "simulator",
      { for e in jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].environment : e.name => e.value }["SIMULATOR_GRPC_PORT"] == "9090",
    ])
    error_message = "The backend container must reach the simulator at simulator:9090 (SIMULATOR_GRPC_HOST/SIMULATOR_GRPC_PORT)."
  }

  assert {
    condition = alltrue([
      jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].portMappings[0].name == "grpc",
      jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].portMappings[0].containerPort == 9090,
      jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].portMappings[0].appProtocol == "grpc",
    ])
    error_message = "The simulator container must expose a named grpc port mapping on 9090 with appProtocol grpc for Service Connect."
  }

  assert {
    condition = alltrue([
      length([for e in jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].environment : e.name if strcontains(upper(e.name), "SQS")]) == 0,
      length([for e in jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].environment : e.name if strcontains(upper(e.name), "SQS")]) == 0,
    ])
    error_message = "No container may carry messaging-queue environment variables any more."
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].logConfiguration.options["awslogs-group"] == aws_cloudwatch_log_group.backend.name
    error_message = "The backend container must log to its dedicated CloudWatch log group."
  }
}

run "services" {
  command = apply

  override_resource {
    target = aws_iam_role.ecs_task_execution
    values = {
      arn = "arn:aws:iam::123456789012:role/baseball-orders-dev-ecs-task-execution"
    }
  }

  override_resource {
    target = aws_iam_role.backend_task
    values = {
      arn = "arn:aws:iam::123456789012:role/baseball-orders-dev-backend-task"
    }
  }

  override_resource {
    target = aws_iam_role.simulator_task
    values = {
      arn = "arn:aws:iam::123456789012:role/baseball-orders-dev-simulator-task"
    }
  }

  override_data {
    target = data.aws_availability_zones.available
    values = {
      names = ["ap-northeast-1a", "ap-northeast-1c"]
    }
  }

  override_data {
    target = data.aws_iam_policy_document.ecs_tasks_assume_role
    values = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"sts:AssumeRole\",\"Principal\":{\"Service\":\"ecs-tasks.amazonaws.com\"}}]}"
    }
  }

  override_data {
    target = data.aws_route53_zone.main
    values = {
      zone_id = "Z1234567890ABC"
      name    = "example.com"
    }
  }

  override_resource {
    target          = aws_acm_certificate.main
    override_during = plan
    values = {
      arn                 = "arn:aws:acm:ap-northeast-1:123456789012:certificate/11111111-2222-3333-4444-555555555555"
      id                  = "arn:aws:acm:ap-northeast-1:123456789012:certificate/11111111-2222-3333-4444-555555555555"
      status              = "PENDING_VALIDATION"
      not_before          = ""
      not_after           = ""
      renewal_eligibility = "INELIGIBLE"
      key_algorithm       = "RSA_2048"
      type                = "AMAZON_ISSUED"
      region              = "ap-northeast-1"
      domain_validation_options = [
        {
          domain_name           = "orders.example.com"
          resource_record_name  = "_abc123.orders.example.com."
          resource_record_type  = "CNAME"
          resource_record_value = "_xyz456.acm-validations.aws."
        },
      ]
    }
  }

  override_resource {
    target = aws_lb.main
    values = {
      arn = "arn:aws:elasticloadbalancing:ap-northeast-1:123456789012:loadbalancer/app/baseball-orders-dev-alb/1234567890abcdef"
    }
  }

  override_resource {
    target = aws_lb_target_group.backend
    values = {
      arn = "arn:aws:elasticloadbalancing:ap-northeast-1:123456789012:targetgroup/baseball-orders-dev-backend/1234567890abcdef"
    }
  }

  assert {
    condition     = aws_ecs_service.backend.desired_count == 1 && aws_ecs_service.backend.launch_type == "FARGATE"
    error_message = "The backend service must run exactly 1 task on standard Fargate (docs/aws-deployment.md: backend is 1 task fixed)."
  }

  assert {
    condition     = aws_ecs_service.backend.network_configuration[0].assign_public_ip == true
    error_message = "The backend task must have a public IP; this configuration has no NAT Gateway (docs/aws-deployment-low-cost.md)."
  }

  assert {
    condition     = contains(aws_ecs_service.backend.network_configuration[0].security_groups, aws_security_group.backend.id)
    error_message = "The backend service must use the backend security group."
  }

  assert {
    condition     = one(aws_ecs_service.backend.load_balancer).target_group_arn == aws_lb_target_group.backend.arn && one(aws_ecs_service.backend.load_balancer).container_port == 8080
    error_message = "The backend service must register with the ALB target group on port 8080."
  }

  assert {
    condition     = aws_ecs_service.simulator.desired_count == 1
    error_message = "The simulator service must start with the documented minimum of 1 task."
  }

  assert {
    condition     = one(aws_ecs_service.simulator.capacity_provider_strategy).capacity_provider == "FARGATE_SPOT" && one(aws_ecs_service.simulator.capacity_provider_strategy).weight == 100
    error_message = "The simulator service must run entirely on Fargate Spot to keep the always-on cost low."
  }

  assert {
    condition     = aws_ecs_service.simulator.network_configuration[0].assign_public_ip == true
    error_message = "The simulator task must have a public IP; this configuration has no NAT Gateway (docs/aws-deployment-low-cost.md)."
  }

  assert {
    condition     = contains(aws_ecs_service.simulator.network_configuration[0].security_groups, aws_security_group.simulator.id)
    error_message = "The simulator service must use the simulator security group (inbound gRPC only from the backend)."
  }

  assert {
    condition     = length(aws_ecs_service.simulator.load_balancer) == 0
    error_message = "The simulator service has no HTTP endpoint and must not be registered with the ALB."
  }

  assert {
    condition     = aws_ecs_service.backend.service_connect_configuration[0].enabled && length(aws_ecs_service.backend.service_connect_configuration[0].service) == 0
    error_message = "The backend service must join Service Connect as a client only."
  }

  assert {
    condition = alltrue([
      aws_ecs_service.simulator.service_connect_configuration[0].enabled,
      one(aws_ecs_service.simulator.service_connect_configuration[0].service).port_name == "grpc",
      one(aws_ecs_service.simulator.service_connect_configuration[0].service).discovery_name == "simulator",
      one(one(aws_ecs_service.simulator.service_connect_configuration[0].service).client_alias).dns_name == "simulator",
      one(one(aws_ecs_service.simulator.service_connect_configuration[0].service).client_alias).port == 9090,
    ])
    error_message = "The simulator service must publish the grpc port through Service Connect as simulator:9090."
  }
}

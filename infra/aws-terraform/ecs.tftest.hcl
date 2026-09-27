mock_provider "aws" {}

variables {
  domain_name       = "orders.example.com"
  route53_zone_name = "example.com"
}

run "cluster_and_task_definitions" {
  command = apply

  override_resource {
    target = aws_appautoscaling_policy.simulator_scale_out
    values = {
      arn = "arn:aws:autoscaling:ap-northeast-1:123456789012:scalingPolicy:11111111-1111-1111-1111-111111111111:resource/ecs/service/baseball-orders-dev-cluster/baseball-orders-dev-simulator:policyName/baseball-orders-dev-simulator-scale-out"
    }
  }

  override_resource {
    target = aws_appautoscaling_policy.simulator_scale_in
    values = {
      arn = "arn:aws:autoscaling:ap-northeast-1:123456789012:scalingPolicy:22222222-2222-2222-2222-222222222222:resource/ecs/service/baseball-orders-dev-cluster/baseball-orders-dev-simulator:policyName/baseball-orders-dev-simulator-scale-in"
    }
  }

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
    target = data.aws_iam_policy_document.backend_sqs
    values = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }

  override_data {
    target = data.aws_iam_policy_document.simulator_sqs
    values = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
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
    target = aws_iam_policy.backend_sqs
    values = {
      arn = "arn:aws:iam::123456789012:policy/baseball-orders-dev-backend-sqs"
    }
  }

  override_resource {
    target = aws_iam_policy.simulator_sqs
    values = {
      arn = "arn:aws:iam::123456789012:policy/baseball-orders-dev-simulator-sqs"
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
    condition     = aws_ecs_task_definition.backend.cpu == "512" && aws_ecs_task_definition.backend.memory == "1024"
    error_message = "The backend task must request 0.5 vCPU / 1 GB, per docs/aws-deployment.md."
  }

  assert {
    condition     = aws_ecs_task_definition.simulator.cpu == "1024" && aws_ecs_task_definition.simulator.memory == "2048"
    error_message = "The simulator task must request 1 vCPU / 2 GB, per docs/aws-deployment.md."
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].image == "${aws_ecr_repository.backend.repository_url}:latest"
    error_message = "The backend container must pull its default image from the backend ECR repository."
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
    condition = contains(
      [for e in jsondecode(aws_ecs_task_definition.simulator.container_definitions)[0].environment : e.name],
      "SIMULATION_SQS_POLL_FIXED_DELAY"
    )
    error_message = "The simulator container must set SIMULATION_SQS_POLL_FIXED_DELAY explicitly; the application-prod.yml default (60s) exceeds the backend's 30s wait (docs/aws-deployment.md)."
  }

  assert {
    condition     = jsondecode(aws_ecs_task_definition.backend.container_definitions)[0].logConfiguration.options["awslogs-group"] == aws_cloudwatch_log_group.backend.name
    error_message = "The backend container must log to its dedicated CloudWatch log group."
  }
}

run "services" {
  command = apply

  override_resource {
    target = aws_appautoscaling_policy.simulator_scale_out
    values = {
      arn = "arn:aws:autoscaling:ap-northeast-1:123456789012:scalingPolicy:11111111-1111-1111-1111-111111111111:resource/ecs/service/baseball-orders-dev-cluster/baseball-orders-dev-simulator:policyName/baseball-orders-dev-simulator-scale-out"
    }
  }

  override_resource {
    target = aws_appautoscaling_policy.simulator_scale_in
    values = {
      arn = "arn:aws:autoscaling:ap-northeast-1:123456789012:scalingPolicy:22222222-2222-2222-2222-222222222222:resource/ecs/service/baseball-orders-dev-cluster/baseball-orders-dev-simulator:policyName/baseball-orders-dev-simulator-scale-in"
    }
  }

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
    target = data.aws_iam_policy_document.backend_sqs
    values = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
    }
  }

  override_data {
    target = data.aws_iam_policy_document.simulator_sqs
    values = {
      json = "{\"Version\":\"2012-10-17\",\"Statement\":[]}"
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
    target = aws_iam_policy.backend_sqs
    values = {
      arn = "arn:aws:iam::123456789012:policy/baseball-orders-dev-backend-sqs"
    }
  }

  override_resource {
    target = aws_iam_policy.simulator_sqs
    values = {
      arn = "arn:aws:iam::123456789012:policy/baseball-orders-dev-simulator-sqs"
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
    error_message = "The simulator service must use the simulator security group (no inbound rules)."
  }

  assert {
    condition     = length(aws_ecs_service.simulator.load_balancer) == 0
    error_message = "The simulator service has no HTTP endpoint and must not be registered with the ALB."
  }
}

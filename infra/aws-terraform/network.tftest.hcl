mock_provider "aws" {}

variables {
  domain_name       = "orders.example.com"
  route53_zone_name = "example.com"
}

run "network_configuration" {
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

  assert {
    condition     = aws_vpc.main.cidr_block == "10.0.0.0/16"
    error_message = "The VPC must use the documented 10.0.0.0/16 CIDR block."
  }

  assert {
    condition     = length(aws_subnet.public) == 2
    error_message = "Exactly two public subnets must be created, one per availability zone."
  }

  assert {
    condition     = alltrue([for s in aws_subnet.public : s.map_public_ip_on_launch])
    error_message = "Every public subnet must auto-assign public IPs, since ECS tasks run there directly."
  }

  assert {
    condition     = alltrue([for s in aws_subnet.public : s.vpc_id == aws_vpc.main.id])
    error_message = "Every public subnet must belong to the VPC created for this deployment."
  }

  assert {
    condition     = length(distinct(aws_subnet.public[*].availability_zone)) == 2
    error_message = "The two public subnets must be spread across two distinct availability zones."
  }

  assert {
    condition     = aws_route_table.public.vpc_id == aws_vpc.main.id
    error_message = "The public route table must belong to the deployment VPC."
  }

  assert {
    condition     = [for r in aws_route_table.public.route : r.gateway_id][0] != null
    error_message = "The public route table must route 0.0.0.0/0 through the Internet Gateway (no NAT Gateway in this configuration)."
  }

  assert {
    condition     = length([for s in aws_subnet.public : s if !can(regex("^private", s.tags.Name))]) == 2
    error_message = "No subnet in this configuration may be tagged as a private subnet; this configuration has no NAT Gateway."
  }
}

run "security_groups" {
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

  assert {
    condition     = aws_security_group.alb.vpc_id == aws_vpc.main.id
    error_message = "The ALB security group must belong to the deployment VPC."
  }

  assert {
    condition     = aws_security_group.backend.vpc_id == aws_vpc.main.id
    error_message = "The backend security group must belong to the deployment VPC."
  }

  assert {
    condition     = aws_security_group.simulator.vpc_id == aws_vpc.main.id
    error_message = "The simulator security group must belong to the deployment VPC."
  }

  assert {
    condition     = aws_vpc_security_group_ingress_rule.alb_https.security_group_id == aws_security_group.alb.id
    error_message = "The ALB must accept inbound HTTPS."
  }

  assert {
    condition     = aws_vpc_security_group_ingress_rule.alb_https.from_port == 443 && aws_vpc_security_group_ingress_rule.alb_https.to_port == 443
    error_message = "The ALB ingress rule must allow only port 443."
  }

  assert {
    condition     = aws_vpc_security_group_ingress_rule.backend_from_alb.referenced_security_group_id == aws_security_group.alb.id
    error_message = "The backend security group must accept inbound traffic only from the ALB security group."
  }

  assert {
    condition     = aws_vpc_security_group_ingress_rule.backend_from_alb.from_port == 8080 && aws_vpc_security_group_ingress_rule.backend_from_alb.to_port == 8080
    error_message = "The backend ingress rule must allow only port 8080, matching the container port."
  }
}

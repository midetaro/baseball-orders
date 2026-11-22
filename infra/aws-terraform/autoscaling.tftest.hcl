mock_provider "aws" {}

variables {
  domain_name       = "orders.example.com"
  route53_zone_name = "example.com"
}

run "simulator_cpu_scaling" {
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
    condition     = aws_appautoscaling_target.simulator.resource_id == "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.simulator.name}"
    error_message = "The autoscaling target must track the simulator ECS service."
  }

  assert {
    condition     = aws_appautoscaling_target.simulator.min_capacity == 1
    error_message = "The simulator must never scale below 1 task (0->1 startup takes 1-2 minutes, exceeding the backend's 30s wait, docs/aws-deployment.md)."
  }

  assert {
    condition     = aws_appautoscaling_target.simulator.max_capacity == var.simulator_max_count
    error_message = "The autoscaling target's max_capacity must be driven by the configured variable."
  }

  assert {
    condition     = aws_appautoscaling_policy.simulator_cpu.policy_type == "TargetTrackingScaling"
    error_message = "The simulator must scale with a target-tracking policy."
  }

  assert {
    condition = alltrue([
      one(one(aws_appautoscaling_policy.simulator_cpu.target_tracking_scaling_policy_configuration).predefined_metric_specification).predefined_metric_type == "ECSServiceAverageCPUUtilization",
      one(aws_appautoscaling_policy.simulator_cpu.target_tracking_scaling_policy_configuration).target_value == var.simulator_cpu_target_percent,
    ])
    error_message = "The simulator policy must track ECS service-average CPU utilization at the configured target."
  }
}

mock_provider "aws" {}

variables {
  domain_name       = "orders.example.com"
  route53_zone_name = "example.com"
}

run "task_roles" {
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

  assert {
    condition     = jsondecode(aws_iam_role.ecs_task_execution.assume_role_policy).Statement[0].Principal.Service == "ecs-tasks.amazonaws.com"
    error_message = "The task execution role must be assumable only by ECS tasks."
  }

  assert {
    condition     = aws_iam_role_policy_attachment.ecs_task_execution.policy_arn == "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
    error_message = "The task execution role must have the AWS-managed ECS task execution policy attached (ECR pull + CloudWatch Logs)."
  }

  assert {
    condition     = jsondecode(aws_iam_role.backend_task.assume_role_policy).Statement[0].Principal.Service == "ecs-tasks.amazonaws.com"
    error_message = "The backend task role must be assumable only by ECS tasks."
  }

  assert {
    condition     = aws_iam_role_policy_attachment.backend_task_sqs.policy_arn == aws_iam_policy.backend_sqs.arn
    error_message = "The backend task role must carry the existing least-privilege backend SQS policy."
  }

  assert {
    condition     = jsondecode(aws_iam_role.simulator_task.assume_role_policy).Statement[0].Principal.Service == "ecs-tasks.amazonaws.com"
    error_message = "The simulator task role must be assumable only by ECS tasks."
  }

  assert {
    condition     = aws_iam_role_policy_attachment.simulator_task_sqs.policy_arn == aws_iam_policy.simulator_sqs.arn
    error_message = "The simulator task role must carry the existing least-privilege simulator SQS policy."
  }
}

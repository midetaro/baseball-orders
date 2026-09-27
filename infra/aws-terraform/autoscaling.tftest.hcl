mock_provider "aws" {}

variables {
  domain_name       = "orders.example.com"
  route53_zone_name = "example.com"
}

run "simulator_queue_depth_scaling" {
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
    condition     = aws_cloudwatch_metric_alarm.simulator_scale_out.dimensions["QueueName"] == aws_sqs_queue.simulation_request.name
    error_message = "The scale-out alarm must watch the simulation-request queue depth (ApproximateNumberOfMessagesVisible)."
  }

  assert {
    condition     = aws_cloudwatch_metric_alarm.simulator_scale_out.metric_name == "ApproximateNumberOfMessagesVisible" && aws_cloudwatch_metric_alarm.simulator_scale_out.namespace == "AWS/SQS"
    error_message = "The scale-out alarm must be based on the request queue's visible message count."
  }

  assert {
    condition     = contains(aws_cloudwatch_metric_alarm.simulator_scale_out.alarm_actions, aws_appautoscaling_policy.simulator_scale_out.arn)
    error_message = "The scale-out alarm must trigger the scale-out Application Auto Scaling policy."
  }

  assert {
    condition     = contains(aws_cloudwatch_metric_alarm.simulator_scale_in.alarm_actions, aws_appautoscaling_policy.simulator_scale_in.arn)
    error_message = "The scale-in alarm must trigger the scale-in Application Auto Scaling policy."
  }

  assert {
    condition     = one(one(aws_appautoscaling_policy.simulator_scale_out.step_scaling_policy_configuration).step_adjustment).scaling_adjustment > 0
    error_message = "The scale-out policy must increase desired count when the request queue backs up."
  }

  assert {
    condition     = one(one(aws_appautoscaling_policy.simulator_scale_in.step_scaling_policy_configuration).step_adjustment).scaling_adjustment < 0
    error_message = "The scale-in policy must decrease desired count once the request queue drains."
  }
}

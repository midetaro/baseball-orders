mock_provider "aws" {}

run "alb_and_certificate" {
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

  variables {
    domain_name       = "orders.example.com"
    route53_zone_name = "example.com"
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
    condition     = aws_lb.main.internal == false
    error_message = "The ALB must be internet-facing so browsers can reach it directly."
  }

  assert {
    condition     = aws_lb.main.idle_timeout == 65
    error_message = "The ALB idle_timeout must be 65s: the backend's 30s synchronous wait plus headroom (docs/aws-deployment.md)."
  }

  assert {
    condition     = length(aws_lb.main.subnets) == 2 && length(setsubtract(aws_subnet.public[*].id, aws_lb.main.subnets)) == 0
    error_message = "The ALB must run in the two public subnets."
  }

  assert {
    condition     = contains(aws_lb.main.security_groups, aws_security_group.alb.id)
    error_message = "The ALB must use the ALB security group (HTTPS inbound only)."
  }

  assert {
    condition     = aws_lb_target_group.backend.port == 8080
    error_message = "The target group must forward to the backend container port 8080."
  }

  assert {
    condition     = aws_lb_target_group.backend.deregistration_delay == "35"
    error_message = "deregistration_delay must be 35s so an in-flight 30s wait is not dropped during deploys (docs/aws-deployment.md)."
  }

  assert {
    condition     = aws_lb_target_group.backend.health_check[0].path == "/login"
    error_message = "The health check must use GET /login: the one path permitAll's in SecurityConfiguration (docs/aws-deployment.md)."
  }

  assert {
    condition     = aws_lb_listener.https.port == 443 && aws_lb_listener.https.protocol == "HTTPS"
    error_message = "The primary listener must serve HTTPS on 443."
  }

  assert {
    condition     = aws_lb_listener.https.default_action[0].target_group_arn == aws_lb_target_group.backend.arn
    error_message = "The HTTPS listener must forward to the backend target group."
  }

  assert {
    condition     = aws_lb_listener.http_redirect.port == 80 && aws_lb_listener.http_redirect.default_action[0].type == "redirect"
    error_message = "Port 80 must redirect to HTTPS, never serve plaintext."
  }

  assert {
    condition     = aws_lb_listener.http_redirect.default_action[0].redirect[0].protocol == "HTTPS" && aws_lb_listener.http_redirect.default_action[0].redirect[0].status_code == "HTTP_301"
    error_message = "The HTTP listener must permanently redirect to HTTPS."
  }

  assert {
    condition     = aws_acm_certificate.main.domain_name == "orders.example.com"
    error_message = "The ACM certificate must cover the configured domain name."
  }

  assert {
    condition     = aws_route53_record.app.zone_id == "Z1234567890ABC" && aws_route53_record.app.name == "orders.example.com"
    error_message = "A Route 53 alias record for the domain must point at the ALB."
  }
}

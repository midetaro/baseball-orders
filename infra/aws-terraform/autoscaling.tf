variable "simulator_max_count" {
  description = "Maximum simulator task count when the simulation-request queue backs up."
  type        = number
  default     = 4
}

variable "simulator_scale_out_threshold" {
  description = "ApproximateNumberOfMessagesVisible on simulation-request that triggers a simulator scale-out."
  type        = number
  default     = 5
}

resource "aws_appautoscaling_target" "simulator" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.simulator.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = 1
  max_capacity       = var.simulator_max_count
}

resource "aws_appautoscaling_policy" "simulator_scale_out" {
  name               = "${local.name_prefix}-simulator-scale-out"
  policy_type        = "StepScaling"
  service_namespace  = aws_appautoscaling_target.simulator.service_namespace
  resource_id        = aws_appautoscaling_target.simulator.resource_id
  scalable_dimension = aws_appautoscaling_target.simulator.scalable_dimension

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 120
    metric_aggregation_type = "Maximum"

    step_adjustment {
      scaling_adjustment          = 1
      metric_interval_lower_bound = 0
    }
  }
}

resource "aws_appautoscaling_policy" "simulator_scale_in" {
  name               = "${local.name_prefix}-simulator-scale-in"
  policy_type        = "StepScaling"
  service_namespace  = aws_appautoscaling_target.simulator.service_namespace
  resource_id        = aws_appautoscaling_target.simulator.resource_id
  scalable_dimension = aws_appautoscaling_target.simulator.scalable_dimension

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 300
    metric_aggregation_type = "Maximum"

    step_adjustment {
      scaling_adjustment          = -1
      metric_interval_upper_bound = 0
    }
  }
}

resource "aws_cloudwatch_metric_alarm" "simulator_scale_out" {
  alarm_name          = "${local.name_prefix}-simulator-queue-depth-high"
  alarm_description   = "The simulation-request queue is backing up; scale the simulator out."
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Average"
  threshold           = var.simulator_scale_out_threshold

  dimensions = {
    QueueName = aws_sqs_queue.simulation_request.name
  }

  alarm_actions = [aws_appautoscaling_policy.simulator_scale_out.arn]
}

resource "aws_cloudwatch_metric_alarm" "simulator_scale_in" {
  alarm_name          = "${local.name_prefix}-simulator-queue-depth-low"
  alarm_description   = "The simulation-request queue has drained; scale the simulator back in."
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 3
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Average"
  threshold           = 0

  dimensions = {
    QueueName = aws_sqs_queue.simulation_request.name
  }

  alarm_actions = [aws_appautoscaling_policy.simulator_scale_in.arn]
}

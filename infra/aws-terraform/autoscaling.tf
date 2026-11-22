variable "simulator_max_count" {
  description = "Maximum simulator task count when simulator CPU utilization stays above simulator_cpu_target_percent."
  type        = number
  default     = 4
}

variable "simulator_cpu_target_percent" {
  description = "Target ECSServiceAverageCPUUtilization (percent) that the simulator service is scaled to hold."
  type        = number
  default     = 60
}

variable "simulator_scale_out_cooldown_seconds" {
  description = "Seconds to wait after a scale-out before another scale-out may start."
  type        = number
  default     = 120
}

variable "simulator_scale_in_cooldown_seconds" {
  description = "Seconds to wait after a scale-in before another scale-in may start."
  type        = number
  default     = 300
}

resource "aws_appautoscaling_target" "simulator" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.simulator.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = 1
  max_capacity       = var.simulator_max_count
}

resource "aws_appautoscaling_policy" "simulator_cpu" {
  name               = "${local.name_prefix}-simulator-cpu"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.simulator.service_namespace
  resource_id        = aws_appautoscaling_target.simulator.resource_id
  scalable_dimension = aws_appautoscaling_target.simulator.scalable_dimension

  target_tracking_scaling_policy_configuration {
    target_value       = var.simulator_cpu_target_percent
    scale_out_cooldown = var.simulator_scale_out_cooldown_seconds
    scale_in_cooldown  = var.simulator_scale_in_cooldown_seconds

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}

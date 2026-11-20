variable "backend_ecr_repository_name" {
  description = "Existing ECR repository that push-ecr.yml pushes backend images to. Terraform reads it and does not create it."
  type        = string
  default     = "baseball-orders/backend"
}

variable "simulator_ecr_repository_name" {
  description = "Existing ECR repository that push-ecr.yml pushes simulator images to. Terraform reads it and does not create it."
  type        = string
  default     = "baseball-orders/simulator"
}

locals {
  ecr_lifecycle_policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after 7 days."
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 7
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Keep only the most recent 30 tagged images."
        selection = {
          tagStatus      = "tagged"
          tagPatternList = ["*"]
          countType      = "imageCountMoreThan"
          countNumber    = 30
        }
        action = {
          type = "expire"
        }
      },
    ]
  })
}

data "aws_ecr_repository" "backend" {
  name = var.backend_ecr_repository_name
}

data "aws_ecr_repository" "simulator" {
  name = var.simulator_ecr_repository_name
}

resource "aws_ecr_lifecycle_policy" "backend" {
  repository = data.aws_ecr_repository.backend.name
  policy     = local.ecr_lifecycle_policy
}

resource "aws_ecr_lifecycle_policy" "simulator" {
  repository = data.aws_ecr_repository.simulator.name
  policy     = local.ecr_lifecycle_policy
}

output "ecr_backend_repository_url" {
  description = "Repository the backend task pulls its image from."
  value       = data.aws_ecr_repository.backend.repository_url
}

output "ecr_simulator_repository_url" {
  description = "Repository the simulator task pulls its image from."
  value       = data.aws_ecr_repository.simulator.repository_url
}

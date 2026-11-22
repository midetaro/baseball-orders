# AWS Terraform

This directory manages the AWS resources used to run `baseball-orders`: the
low-cost deployment (no NAT Gateway) described in
[docs/aws-deployment-low-cost.md](../../docs/aws-deployment-low-cost.md). It
applies only when the backend has no login and no RDBMS persistence, per that
document's applicability conditions. The infrastructure is declared directly
in native Terraform HCL.

## Managed resources

- ECS Service Connect: an `aws_service_discovery_http_namespace` named
  `<prefix>.internal`; the simulator exposes its `grpc` port mapping
  (`simulator_grpc_port`, default 9090, `appProtocol` grpc) with the client
  alias `simulator:9090`, and the backend is a client only. The security group
  rule `simulator_grpc_from_backend` allows TCP 9090 into the simulator from the
  backend security group only
- VPC with two public subnets (one per AZ) and an Internet Gateway; no private
  subnet or NAT Gateway. ECS tasks run in the public subnets with public IPs;
  inbound is blocked at the security group level except ALB -> backend on 8080
- ALB (HTTPS 443, HTTP->HTTPS redirect), ACM certificate with Route 53 DNS
  validation, and an alias record pointing the configured domain at the ALB
- Lifecycle policies (untagged images expire after 7 days; only the most recent
  30 tagged images are kept) on the existing `baseball-orders/backend` and
  `baseball-orders/simulator` ECR repositories. The repositories themselves are
  created outside Terraform and read through data sources; `push-ecr.yml` pushes
  the `main`-tagged images the tasks start from
  (`backend_ecr_repository_name` / `simulator_ecr_repository_name`)
- ECS cluster (Fargate + Fargate Spot), task definitions, and services for the
  backend (0.5 vCPU / 4 GB, 1 fixed task, standard Fargate) and simulator
  (1 vCPU / 4 GB, Fargate Spot, CPU-autoscaled). Both containers cap the JVM heap
  at 2 GB with `JAVA_TOOL_OPTIONS=-Xmx2g` (`task_memory` / `jvm_options`)
- IAM task execution role and least-privilege task roles for the backend and
  simulator workloads
- CloudWatch log groups (14-day retention)
- Application Auto Scaling for the simulator service: CPU target tracking
  (`simulator_cpu_target_percent`, `simulator_scale_out_cooldown_seconds`,
  `simulator_scale_in_cooldown_seconds`)
- IAM policy ARN and ECR repository URL outputs

`rds.tf` is intentionally not implemented here, per the low-cost document's
applicability conditions. The ALB health check uses `GET /`, which the backend
serves without login.

The backend reaches the simulator at `simulator:9090` through the Service
Connect alias (`SIMULATOR_GRPC_HOST` / `SIMULATOR_GRPC_PORT`; the simulator
listens on `SIMULATION_GRPC_PORT`). The task definitions use
`lifecycle.ignore_changes = [container_definitions]`, so an existing deployment
needs a new task definition revision (for example by running the image push and
deploy workflows) before the `grpc` port mapping and these environment variables
take effect.

## Prerequisites

- Terraform 1.8 or newer
- AWS credentials available through the standard AWS credential chain

For local use, copy `terraform.tfvars.example` to `terraform.tfvars` and set
`aws_profile` to the name of an AWS CLI shared-config profile with permission to
manage the S3 state and AWS resources. The local file is intentionally ignored
by Git. When `aws_profile` is not set, Terraform uses the default AWS credential
chain, which is how GitHub Actions receives OIDC credentials.

`domain_name` and `route53_zone_name` have no default and must be set on every
plan/apply (for example `terraform.tfvars`, or `-var` as shown below). A public
Route 53 hosted zone for `route53_zone_name` must already exist; it is looked up
by name, not created here.

## GitHub Actions deployment

`.github/workflows/plan-terraform-deployment.yml` creates a production Terraform
plan for pull requests targeting `develop`; it can also be run manually. It is a
pre-merge check for `.github/workflows/deploy.yml` and never runs `terraform
apply`. `.github/workflows/deploy.yml` applies the production plan after a
Terraform change is pushed to `main`. Both workflows assume that the AWS IAM
OIDC provider for GitHub Actions and the assumable role have already been
configured.

Set these GitHub Actions variables before enabling deployments:

- `AWS_TERRAFORM_ROLE_ARN`: IAM role ARN GitHub Actions assumes through OIDC.
- `TF_STATE_BUCKET`: existing S3 bucket used for Terraform state.
- `DOMAIN_NAME`: fully qualified domain name the ALB serves (`domain_name`).
- `ROUTE53_ZONE_NAME`: existing Route 53 public hosted zone name covering
  `DOMAIN_NAME` (`route53_zone_name`).
- `AWS_REGION` (optional): AWS and state-bucket region; defaults to
  `ap-northeast-1`.
- `TF_STATE_KEY` (optional): state object key; defaults to
  `baseball-orders/production/terraform.tfstate`.

The assumed role needs access to the managed VPC, ALB, ECR, ECS, IAM, Cloud Map
(Service Connect namespace), CloudWatch, Application Auto Scaling, ACM, and Route 53 resources, and
read/write access to the configured state object and its `.tflock` lock object.
The workflows use the protected `aws-production` GitHub Environment, so create
that environment and add any required reviewers before the first deployment.

To let `release-ecr.yml` push release builds to the same repositories, set the
`ECR_BACKEND_REPOSITORY` and `ECR_SIMULATOR_REPOSITORY` GitHub Actions variables
to `baseball-orders/backend` / `baseball-orders/simulator`.

## Usage

```sh
terraform fmt -check
terraform init
terraform validate
terraform test
terraform plan -var='environment=dev' -var='domain_name=orders.example.com' -var='route53_zone_name=example.com'
terraform apply -var='environment=dev' -var='domain_name=orders.example.com' -var='route53_zone_name=example.com'
```

For a shared or production environment, configure a remote Terraform backend
before the first apply. Backend settings are deployment-specific and are not
hard-coded here, so credentials and state bucket details are never committed.

Edit `main.tf` directly and run `terraform fmt -check`, `terraform validate`, and
`terraform test` before committing changes.

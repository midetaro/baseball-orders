# AWS Terraform

This directory manages the AWS resources used to run `baseball-orders`: the
low-cost deployment (no NAT Gateway) described in
[docs/aws-deployment-low-cost.md](../../docs/aws-deployment-low-cost.md). It
applies only when Google SSO and RDBMS persistence are both unused, per that
document's applicability conditions. The infrastructure is declared directly
in native Terraform HCL.

## Managed resources

- `simulation-request` and `simulation-result` SQS standard queues, each with
  a dead-letter queue (maximum receive count 5), SQS-managed server-side
  encryption, and long polling
- VPC with two public subnets (one per AZ) and an Internet Gateway; no private
  subnet or NAT Gateway. ECS tasks run in the public subnets with public IPs;
  inbound is blocked at the security group level except ALB -> backend on 8080
- ALB (HTTPS 443, HTTP->HTTPS redirect), ACM certificate with Route 53 DNS
  validation, and an alias record pointing the configured domain at the ALB
- ECR repositories for the backend and simulator images, with a lifecycle
  policy (untagged images expire after 7 days; only the most recent 30 tagged
  images are kept)
- ECS cluster (Fargate + Fargate Spot), task definitions, and services for the
  backend (0.5 vCPU / 1 GB, 1 fixed task, standard Fargate) and simulator
  (1 vCPU / 2 GB, Fargate Spot, autoscaled)
- IAM task execution role and least-privilege task roles for the backend and
  simulator workloads
- CloudWatch log groups (14-day retention) and alarms on both DLQs
- Application Auto Scaling for the simulator service, driven by
  `ApproximateNumberOfMessagesVisible` on `simulation-request`
- Queue URL/ARN, IAM policy ARN, and ECR repository URL outputs

Google SSO wiring (`GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` via SSM Parameter
Store) and `rds.tf` are intentionally not implemented here, per the low-cost
document's applicability conditions.

The default queue names match the names currently used by the backend Java
application. Override `request_queue_name` and `result_queue_name` only when the
application configuration is changed at the same time.

Set the same queue names for both local applications when overriding the
Terraform defaults:

```sh
export SIMULATION_REQUEST_QUEUE_NAME=my-simulation-request
export SIMULATION_RESULT_QUEUE_NAME=my-simulation-result
```

Both applications default to `simulation-request` and `simulation-result` when
these environment variables are absent.

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

The assumed role needs access to the managed VPC, ALB, ECR, ECS, IAM, SQS,
CloudWatch, Application Auto Scaling, ACM, and Route 53 resources, and
read/write access to the configured state object and its `.tflock` lock object.
The workflows use the protected `aws-production` GitHub Environment, so create
that environment and add any required reviewers before the first deployment.

After the first apply, set the `ECR_BACKEND_REPOSITORY` and
`ECR_SIMULATOR_REPOSITORY` GitHub Actions variables (used by
`release-ecr.yml`) to `${project_name}-${environment}-backend` /
`${project_name}-${environment}-simulator` (for example
`baseball-orders-production-backend`) — the repository name portion of the
`ecr_backend_repository_url` / `ecr_simulator_repository_url` outputs — so
release builds push to the repositories this configuration creates.

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

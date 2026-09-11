# AWS Terraform

This directory manages the AWS messaging resources used by `baseball-orders`.
The infrastructure is declared directly in native Terraform HCL.

## Managed resources

- `simulation-request` and `simulation-result` SQS standard queues
- A dead-letter queue for each queue, with a maximum receive count of five
- SQS-managed server-side encryption and long polling
- Least-privilege IAM policies for the backend and simulator workloads
- Queue URL, queue ARN, and IAM policy ARN outputs

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

## GitHub Actions deployment

`.github/workflows/apply-terraform.yml` plans and applies infrastructure after a
Terraform change is pushed to `develop`; it can also be run manually. It assumes
that the AWS IAM OIDC provider for GitHub Actions and the assumable role have
already been configured.

Set these GitHub Actions variables before enabling deployments:

- `AWS_TERRAFORM_ROLE_ARN`: IAM role ARN GitHub Actions assumes through OIDC.
- `TF_STATE_BUCKET`: existing S3 bucket used for Terraform state.
- `AWS_REGION` (optional): AWS and state-bucket region; defaults to
  `ap-northeast-1`.
- `TF_STATE_KEY` (optional): state object key; defaults to
  `baseball-orders/develop/terraform.tfstate`.

The assumed role needs access to the managed SQS and IAM-policy resources, and
read/write access to the configured state object and its `.tflock` lock object.
The workflow uses the protected `aws-development` GitHub Environment, so create
that environment and add any required reviewers before the first deployment.

## Usage

```sh
terraform fmt -check
terraform init
terraform validate
terraform test
terraform plan -var='environment=dev'
terraform apply -var='environment=dev'
```

For a shared or production environment, configure a remote Terraform backend
before the first apply. Backend settings are deployment-specific and are not
hard-coded here, so credentials and state bucket details are never committed.

Edit `main.tf` directly and run `terraform fmt -check`, `terraform validate`, and
`terraform test` before committing changes.

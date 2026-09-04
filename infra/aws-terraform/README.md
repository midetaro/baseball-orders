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

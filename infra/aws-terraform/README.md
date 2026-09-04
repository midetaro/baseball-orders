# AWS Terraform (Go generator)

This directory manages the AWS messaging resources used by `baseball-orders`.
The Go program uses only the standard library and emits native Terraform HCL.

CDK for Terraform is intentionally not used because HashiCorp ended support for
CDKTF on December 10, 2025. The generated file can be managed with the regular
Terraform CLI without a CDKTF runtime dependency.

## Managed resources

- `simulation-request` and `simulation-result` SQS standard queues
- A dead-letter queue for each queue, with a maximum receive count of five
- SQS-managed server-side encryption and long polling
- Least-privilege IAM policies for the backend and simulator workloads
- Queue URL, queue ARN, and IAM policy ARN outputs

The default queue names match the names currently used by the backend Java
application. Override `request_queue_name` and `result_queue_name` only when the
application configuration is changed at the same time.

## Prerequisites

- Go 1.23 or newer
- Terraform 1.8 or newer
- AWS credentials available through the standard AWS credential chain

## Usage

Generate the native Terraform configuration:

```sh
go run . > main.tf
terraform fmt -check
terraform init
terraform validate
terraform plan -var='environment=dev'
terraform apply -var='environment=dev'
```

For a shared or production environment, configure a remote Terraform backend
before the first apply. Backend settings are deployment-specific and are not
hard-coded here, so credentials and state bucket details are never committed.

Run the generator tests with:

```sh
go test ./...
```

Regenerate `main.tf` whenever `generator.go` changes. Do not manually edit the
generated file.

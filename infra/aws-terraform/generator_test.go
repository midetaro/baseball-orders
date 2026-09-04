package main

import (
	"io"
	"os"
	"strings"
	"testing"
)

func TestRenderTerraform(t *testing.T) {
	terraform := RenderTerraform()

	wants := []string{
		`required_version = ">= 1.8.0"`,
		`source  = "hashicorp/aws"`,
		`version = "~> 6.0"`,
		`default     = "ap-northeast-1"`,
		`resource "aws_sqs_queue" "simulation_request"`,
		`name                       = var.request_queue_name`,
		`resource "aws_sqs_queue" "simulation_result"`,
		`resource "aws_sqs_queue" "simulation_request_dlq"`,
		`resource "aws_sqs_queue" "simulation_result_dlq"`,
		`sqs_managed_sse_enabled    = true`,
		`maxReceiveCount     = 5`,
		`resource "aws_iam_policy" "backend_sqs"`,
		`resource "aws_iam_policy" "simulator_sqs"`,
		`output "simulation_request_queue_url"`,
		`output "simulation_result_queue_url"`,
	}
	for _, want := range wants {
		if !strings.Contains(terraform, want) {
			t.Errorf("generated Terraform does not contain %q", want)
		}
	}
}

func TestRenderTerraformIsDeterministic(t *testing.T) {
	first := RenderTerraform()
	second := RenderTerraform()

	if first != second {
		t.Error("RenderTerraform() generated different output for identical input")
	}
}

func TestMainWritesTerraformToStandardOutput(t *testing.T) {
	reader, writer, err := os.Pipe()
	if err != nil {
		t.Fatalf("os.Pipe() error = %v", err)
	}
	originalStdout := os.Stdout
	os.Stdout = writer
	t.Cleanup(func() { os.Stdout = originalStdout })

	main()
	if err := writer.Close(); err != nil {
		t.Fatalf("writer.Close() error = %v", err)
	}
	output, err := io.ReadAll(reader)
	if err != nil {
		t.Fatalf("io.ReadAll() error = %v", err)
	}

	if string(output) != RenderTerraform() {
		t.Error("main() did not write the generated Terraform configuration")
	}
}

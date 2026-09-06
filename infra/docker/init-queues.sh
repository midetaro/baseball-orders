#!/bin/sh
set -eu

for queue in "$SIMULATION_REQUEST_QUEUE_NAME" "$SIMULATION_RESULT_QUEUE_NAME"; do
    curl --fail-with-body --silent --show-error \
        --connect-timeout 5 --max-time 15 \
        --aws-sigv4 "aws:amz:${AWS_REGION}:sqs" \
        --user "${AWS_ACCESS_KEY_ID}:${AWS_SECRET_ACCESS_KEY}" \
        --data-urlencode Action=CreateQueue \
        --data-urlencode Version=2012-11-05 \
        --data-urlencode "QueueName=${queue}" \
        "$AWS_ENDPOINT_URL_SQS"
done

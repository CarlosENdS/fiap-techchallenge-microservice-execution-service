#!/bin/bash
# LocalStack SQS Queue Initialization Script
# This script creates all required SQS queues for local development and testing

echo "Initializing AWS SQS queues in LocalStack..."

# Wait for LocalStack to be ready
sleep 5

# Create FIFO queue for outbound execution events
awslocal sqs create-queue \
  --queue-name execution-service-events.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

# Create FIFO DLQ for execution events
awslocal sqs create-queue \
  --queue-name execution-service-events-dlq.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

# Create standard queues for outbound notifications
awslocal sqs create-queue --queue-name execution-completed-queue
awslocal sqs create-queue --queue-name resource-unavailable-queue

# Create FIFO queues for inbound events (consumed by this service)
awslocal sqs create-queue \
  --queue-name billing-events.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

awslocal sqs create-queue \
  --queue-name os-order-events-queue.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false

echo "SQS queues created successfully!"

# List all queues
echo "Available queues:"
awslocal sqs list-queues

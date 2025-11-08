#!/bin/bash

# Stop the script if any command fails
set -e

# Config
STACK_NAME="patient-management"
BUCKET_NAME="cf-templates"
TEMPLATE_KEY="patient-stack.template.json"
TEMPLATE_FILE="./cdk.out/$TEMPLATE_KEY"
REGION="us-east-1"
LOCALSTACK_URL="http://localhost:4566"
TEMPLATE_URL="$LOCALSTACK_URL/$BUCKET_NAME/$TEMPLATE_KEY"


# Required for LocalStack CLI Compatibility
export AWS_DEFAULT_REGION=$REGION
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test


echo "Ensuring S3 bucket for templates exists..."
if ! aws --endpoint-url=$LOCALSTACK_URL s3api head-bucket --bucket $BUCKET_NAME 2>/dev/null; then
  echo "Bucket not found, creating s3://$BUCKET_NAME to store CloudFormation templates..."
  aws --endpoint-url=$LOCALSTACK_URL s3 mb s3://$BUCKET_NAME
fi

echo "Uploading CloudFormation template to S3..."
aws --endpoint-url=$LOCALSTACK_URL s3 cp "$TEMPLATE_FILE" "s3://$BUCKET_NAME/$TEMPLATE_KEY"

if aws --endpoint-url=$LOCALSTACK_URL cloudformation describe-stacks --stack-name $STACK_NAME >/dev/null 2>&1; then
   echo "Deleting existing CloudFormation stack '$STACK_NAME'..."
   aws  --endpoint-url=$LOCALSTACK_URL cloudformation delete-stack --stack-name $STACK_NAME
fi


echo "Creating CloudFormation stack '$STACK_NAME'..."
aws --endpoint-url=$LOCALSTACK_URL cloudformation create-stack \
    --stack-name $STACK_NAME \
    --template-url $TEMPLATE_URL

echo "Waiting for stack creation to complete..."
aws --endpoint-url=$LOCALSTACK_URL cloudformation wait stack-create-complete \
    --stack-name $STACK_NAME


echo "Looking for last ALB (api-gateway)..."
aws --endpoint-url=$LOCALSTACK_URL elbv2 describe-load-balancers \
    --query "LoadBalancers[-1].DNSName" --output text
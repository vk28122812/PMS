#!/bin/bash

set -e # Stops the script if any command fails

BUCKET_NAME="patient-management-templates"
TEMPLATE_KEY="localstack.template.json"
TEMPLATE_FILE="./cdk.out/$TEMPLATE_KEY"



# Cleanup existing stack
aws  --endpoint-url=http://localhost:4566 cloudformation delete-stack \
    --stack-name patient-management
#
#if ! aws --endpoint-url=http://localhost:4566 s3api head-bucket --bucket $BUCKET_NAME  2>/dev/null; then
#
#  aws --endpoint-url=http://localhost:4566 s3api create-bucket \
#    --bucket $BUCKET_NAME
#
#fi
#
#aws --endpoint-url=http://localhost:4566 s3api put-object \
#    --bucket $BUCKET_NAME \
#    --key $TEMPLATE_KEY \
#    --body $TEMPLATE_FILE
#
#aws  --endpoint-url=http://localhost:4566 cloudformation create-stack \
#    --stack-name patient-management \
#    --template-url "http://localstack:4566/$BUCKET_NAME/$TEMPLATE_KEY"
#
#aws --endpoint-url=http://localhost:4566 cloudformation wait stack-create-complete \
#    --stack-name patient-management

aws --endpoint-url=http://localhost:4566 cloudformation deploy \
    --stack-name patient-management \
    --template-file $TEMPLATE_FILE

# Get Api Gateway DNS name
aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text
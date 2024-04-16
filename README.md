# aws-sam-crud-api-example
The purpose of this project is to demonstrate the usage of serverless AWS services - Lambda, DynamoDB and API Gateway to create a simple CRUD API application.
AWS SAM framework is used to facilitate application deployment.

## Tech Stack
JDK 21, AWS SDK, JUnit 5, Mockito, Testcontainers, GSON, Maven, AWS SAM

## Description
Each API method has a dedicated Lambda function named after its functionality, but there are two additional modules `user-core` and `lambda-layer`.
`user-core` contains common code required by other functions, and that code is provide to other user functions via `lambda-layer` which also contains common dependencies.

All features are covered by integration tests (via Testcontainers and localstack image).  
AWS resources are defined in the `template.yaml` file.

## Architecture diagram

![Alt text](./resources/aws-diagram.png?raw=true)

## Build and deployment
To build and deploy run the following in a shell:

```bash
sam validate
sam build
sam deploy --guided
```
The first command will validate the template file.  
The second command will build the source of your application.  
The third command will package and deploy the application to AWS, with a series of prompts.

## Cleanup
To delete the application (the created stack) from an AWS account, run the following:

```bash
sam delete --stack-name name-of-the-stack
```

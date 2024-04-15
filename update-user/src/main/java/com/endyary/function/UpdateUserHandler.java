package com.endyary.function;


import com.endyary.core.DynamoDbProvider;
import com.endyary.core.User;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Handler for requests to Lambda function.
 */
public class UpdateUserHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbEnhancedClient dbClient;
  private final String tableName;
  private final TableSchema<User> userTableSchema;

  public UpdateUserHandler() {
    dbClient = DynamoDbProvider.getEnhancedClient();
    tableName = DynamoDbProvider.getTableName();
    userTableSchema = TableSchema.fromBean(User.class);
  }

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent request,
      final Context context) {
    String body = request.getBody();
    Map<String, String> pathParameters = request.getPathParameters();
    String response = "";
    int statusCode = HttpStatusCode.BAD_REQUEST;

    if (pathParameters != null && StringUtils.isNotBlank(body)) {
      Gson gson = new Gson();
      User user = gson.fromJson(body, User.class);
      if (user != null) {
        DynamoDbTable<User> userTable = dbClient.table(tableName, userTableSchema);
        user.setId(pathParameters.get("id"));

        Expression expression = Expression.builder()
            .expression("Id = :id")
            .expressionValues(Map.of(":id", AttributeValue.fromS(user.getId())))
            .build();
        UpdateItemEnhancedRequest<User> userUpdateRequest =
            UpdateItemEnhancedRequest.builder(User.class).conditionExpression(expression)
                .item(user)
                .build();

        try {
          User updateResult = userTable.updateItem(userUpdateRequest);
          response = gson.toJson(updateResult);
          statusCode = HttpStatusCode.OK;
        } catch (ConditionalCheckFailedException checkFailedException) {
          response = String.format("User with id %s not found", user.getId());
          statusCode = HttpStatusCode.NOT_FOUND;
        }
      }
    }
    return new APIGatewayProxyResponseEvent()
        .withStatusCode(statusCode)
        .withHeaders(Collections.emptyMap())
        .withBody(response);
  }
}

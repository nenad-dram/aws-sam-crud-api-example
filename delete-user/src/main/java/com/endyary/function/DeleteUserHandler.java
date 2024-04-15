package com.endyary.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.endyary.core.DynamoDbProvider;
import com.endyary.core.User;
import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Handler for requests to Lambda function.
 */
public class DeleteUserHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbEnhancedClient dbClient;
  private final String tableName;
  private final TableSchema<User> userTableSchema;

  public DeleteUserHandler() {
    dbClient = DynamoDbProvider.getEnhancedClient();
    tableName = DynamoDbProvider.getTableName();
    userTableSchema = TableSchema.fromBean(User.class);
  }

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent request,
      final Context context) {
    String response = "";
    int statusCode = HttpStatusCode.BAD_REQUEST;
    DynamoDbTable<User> userTable = dbClient.table(tableName, userTableSchema);
    Map<String, String> pathParameters = request.getPathParameters();
    if (pathParameters != null) {
      String userId = pathParameters.get("id");
      Expression expression = Expression.builder()
          .expression("Id = :id")
          .expressionValues(Map.of(":id", AttributeValue.fromS(userId)))
          .build();
      DeleteItemEnhancedRequest deleteItemRequest = DeleteItemEnhancedRequest.builder()
          .conditionExpression(expression)
          .key(builder -> builder.partitionValue(userId))
          .build();

      try {
        userTable.deleteItem(deleteItemRequest);
        statusCode = HttpStatusCode.OK;
      } catch (ConditionalCheckFailedException checkFailedException) {
        response = String.format("User with id %s not found", userId);
        statusCode = HttpStatusCode.NOT_FOUND;
      }
    }

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(statusCode)
        .withHeaders(Collections.emptyMap())
        .withBody(response);
  }
}

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
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.HttpStatusCode;

/**
 * Handler for requests to Lambda function.
 */
public class GetUserHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbEnhancedClient dbClient;
  private final String tableName;
  private final TableSchema<User> userTableSchema;

  public GetUserHandler() {
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
      User user = userTable.getItem(Key.builder().partitionValue(userId).build());
      if (user != null) {
        response = new Gson().toJson(user);
        statusCode = HttpStatusCode.OK;
      } else {
        statusCode = HttpStatusCode.NOT_FOUND;
      }
    }

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(statusCode)
        .withHeaders(Collections.emptyMap())
        .withBody(response);
  }
}

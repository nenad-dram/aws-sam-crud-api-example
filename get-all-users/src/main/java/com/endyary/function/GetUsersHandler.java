package com.endyary.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.endyary.core.DynamoDbProvider;
import com.endyary.core.User;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.HttpStatusCode;

/**
 * Handler for requests to Lambda function.
 */
public class GetUsersHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbEnhancedClient dbClient;
  private final String tableName;
  private final TableSchema<User> userTableSchema;

  public GetUsersHandler() {
    dbClient = DynamoDbProvider.getEnhancedClient();
    tableName = DynamoDbProvider.getTableName();
    userTableSchema = TableSchema.fromBean(User.class);
  }

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent request,
      final Context context) {
    DynamoDbTable<User> userTable = dbClient.table(tableName, userTableSchema);
    Gson gson = new Gson();
    List<User> responseList = userTable.scan().items().stream().toList();

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(HttpStatusCode.OK)
        .withHeaders(Collections.emptyMap())
        .withBody(gson.toJson(responseList));
  }
}

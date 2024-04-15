package com.endyary.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.endyary.core.DynamoDbProvider;
import com.endyary.core.User;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.UUID;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.utils.StringUtils;

public class CreateUserHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbEnhancedClient dbClient;
  private final String tableName;
  private final TableSchema<User> userTableSchema;

  public CreateUserHandler() {
    dbClient = DynamoDbProvider.getEnhancedClient();
    tableName = DynamoDbProvider.getTableName();
    userTableSchema = TableSchema.fromBean(User.class);
  }

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent request,
      final Context context) {
    int statusCode = HttpStatusCode.NO_CONTENT;
    String requestBody = request.getBody();
    if (StringUtils.isNotBlank(requestBody)) {
      User user = new Gson().fromJson(requestBody, User.class);
      user.setId(UUID.randomUUID().toString());
      DynamoDbTable<User> userTable = dbClient.table(tableName, userTableSchema);
      userTable.putItem(user);
      statusCode = HttpStatusCode.CREATED;
    }

    return new APIGatewayProxyResponseEvent()
        .withHeaders(Collections.emptyMap())
        .withStatusCode(statusCode);
  }
}

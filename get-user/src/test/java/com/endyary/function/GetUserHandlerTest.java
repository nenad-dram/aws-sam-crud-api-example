package com.endyary.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.endyary.core.DynamoDbProvider;
import com.endyary.core.User;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class GetUserHandlerTest {

  private static DynamoDbEnhancedClient dbClient;
  private static DynamoDbTable<User> dynamoDbTable;
  @Mock
  private Context context;
  @Mock
  private APIGatewayProxyRequestEvent request;

  private static final String TABLE_NAME = "Users";

  @Container
  static LocalStackContainer localStack = new LocalStackContainer(
      DockerImageName.parse("localstack/localstack:latest")

  );

  @BeforeAll
  static void init() {
    dbClient = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(
            DynamoDbClient.builder().endpointOverride(localStack.getEndpoint())
                .build())
        .build();
    dynamoDbTable = dbClient.table(TABLE_NAME, TableSchema.fromBean(User.class));
    dynamoDbTable.createTable();
  }

  @Test
  void shouldReturnUser() {
    User dbUser = createUser();
    Map<String, String> pathParameters = new HashMap<>();
    pathParameters.put("id", dbUser.getId());
    when(request.getPathParameters()).thenReturn(pathParameters);

    try (MockedStatic<DynamoDbProvider> dynamoDbProviderMocked = mockStatic(
        DynamoDbProvider.class)) {
      dynamoDbProviderMocked.when(DynamoDbProvider::getEnhancedClient).thenReturn(dbClient);
      dynamoDbProviderMocked.when(DynamoDbProvider::getTableName).thenReturn(TABLE_NAME);
      GetUserHandler getUserHandler = new GetUserHandler();
      APIGatewayProxyResponseEvent response = getUserHandler.handleRequest(request, context);

      User responseUser = new Gson().fromJson(response.getBody(), User.class);
      assertEquals(dbUser.getFirstName(), responseUser.getFirstName());
      assertEquals(dbUser.getLastName(), responseUser.getLastName());
    }

  }

  private User createUser() {
    User testUser = new User();
    testUser.setFirstName("Test");
    testUser.setLastName("User");
    testUser.setId(UUID.randomUUID().toString());
    dynamoDbTable.putItem(testUser);
    return testUser;
  }
}

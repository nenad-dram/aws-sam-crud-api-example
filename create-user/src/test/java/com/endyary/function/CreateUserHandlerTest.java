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
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class CreateUserHandlerTest {

  private static DynamoDbEnhancedClient dbClient;
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
    DynamoDbTable<User> dynamoDbTable = dbClient.table(TABLE_NAME,
        TableSchema.fromBean(User.class));
    dynamoDbTable.createTable();
  }

  @Test
  void shouldReturnCreated() {
    User testUser = new User();
    testUser.setFirstName("Test");
    testUser.setLastName("User");

    when(request.getBody()).thenReturn(new Gson().toJson(testUser));

    try (MockedStatic<DynamoDbProvider> dynamoDbProviderMocked = mockStatic(
        DynamoDbProvider.class)) {
      dynamoDbProviderMocked.when(DynamoDbProvider::getEnhancedClient).thenReturn(dbClient);
      dynamoDbProviderMocked.when(DynamoDbProvider::getTableName).thenReturn(TABLE_NAME);
      CreateUserHandler createUserHandler = new CreateUserHandler();
      APIGatewayProxyResponseEvent response = createUserHandler.handleRequest(request, context);

      assertEquals(HttpStatusCode.CREATED, response.getStatusCode());
    }
  }
}

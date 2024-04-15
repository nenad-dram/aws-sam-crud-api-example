package com.endyary.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.endyary.core.DynamoDbProvider;
import com.endyary.core.User;
import com.google.gson.Gson;
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
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class GetUsersHandlerTest {

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
  void shouldReturnUsers() {
    insertUsers();

    try (MockedStatic<DynamoDbProvider> dynamoDbProviderMocked = mockStatic(
        DynamoDbProvider.class)) {
      dynamoDbProviderMocked.when(DynamoDbProvider::getEnhancedClient).thenReturn(dbClient);
      dynamoDbProviderMocked.when(DynamoDbProvider::getTableName).thenReturn(TABLE_NAME);
      GetUsersHandler getUsersHandler = new GetUsersHandler();
      APIGatewayProxyResponseEvent response = getUsersHandler.handleRequest(request, context);

      assertEquals(HttpStatusCode.OK, response.getStatusCode());
      User[] users = new Gson().fromJson(response.getBody(), User[].class);
      assertEquals(3, users.length);
    }

  }

  private void insertUsers() {
    User testUser1 = new User();
    testUser1.setFirstName("First");
    testUser1.setLastName("User");
    testUser1.setId(UUID.randomUUID().toString());

    User testUser2 = new User();
    testUser2.setFirstName("Second");
    testUser2.setLastName("User");
    testUser2.setId(UUID.randomUUID().toString());

    User testUser3 = new User();
    testUser3.setFirstName("Third");
    testUser3.setLastName("User");
    testUser3.setId(UUID.randomUUID().toString());

    BatchWriteItemEnhancedRequest writeItemRequest = BatchWriteItemEnhancedRequest.builder()
        .writeBatches(WriteBatch.builder(User.class)
            .mappedTableResource(dynamoDbTable)
            .addPutItem(builder -> builder.item(testUser1))
            .addPutItem(builder -> builder.item(testUser2))
            .addPutItem(builder -> builder.item(testUser3))
            .build())
        .build();

    dbClient.batchWriteItem(writeItemRequest);
  }
}

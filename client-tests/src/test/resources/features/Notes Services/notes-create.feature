#@notes @create @regression
#Feature: Create Note - POST /api/v1/notes
#
#  Background:
#    Given the Notes microservice is running
#    And Document DB is available
#
#  @smoke
#  Scenario Template: Successfully create a user-generated note "<TestCaseID>"
#    Given the user is authenticated with role "SRE"
#    And a valid note creation request with:
#      | orderId  | shipmentId | noteType | noteText                  | noteReason      | entity | userId |
#      | <Order>  | <Ship>     | COMMENT  | Customer follow-up added  | CUSTOMER_CALL  | ORDER  | U123   |
#    When the user sends POST request to "/api/v1/notes"
#    Then the service returns status 201
#    And a unique noteId is generated
#    And an idempotencyKey is assigned
#    And the note is persisted in Document DB
#    And createdAt timestamp is in UTC epoch format
#    And a success log entry is recorded
#
#    Examples:
#      | TestCaseID | Order     | Ship      |
#      | TC_CN_01   | ORD-1001  | SHIP-01   |
#      | TC_CN_02   | ORD-2002  | SHIP-02   |
#
#  @negative
#  Scenario Outline: Missing mandatory field "<Field>"
#    Given the user is authenticated
#    And the request payload is missing "<Field>"
#    When the user sends POST request to "/api/v1/notes"
#    Then the service returns status 400
#    And the error response contains validation message for "<Field>"
#
#    Examples:
#      | Field      |
#      | orderId    |
#      | noteType   |
#      | noteText   |
#      | entity     |
#      | entityKey  |
#
#  @negative
#  Scenario: Payload exceeds maximum allowed note length
#    Given the user is authenticated
#    And the noteText exceeds maximum allowed length
#    When the user sends POST request to "/api/v1/notes"
#    Then the service returns status 400
#    And an error indicates payload size violation
#
#  @security
#  Scenario: Unauthorized user attempts to create note
#    Given no authentication token is provided
#    When the user sends POST request to "/api/v1/notes"
#    Then the service returns status 401
#
#  @security
#  Scenario: Forbidden role attempts to create note
#    Given the user is authenticated with role "READ_ONLY"
#    When the user sends POST request to "/api/v1/notes"
#    Then the service returns status 403

@EventStore @Publish
Feature: Event Store - Event Publishing Validation
  As a system integrator
  I need to publish events to the Event Store via the HTTP Publishing API
  So that domain events are ingested for downstream processing and persistence

  Background:
    Given the Event Store publishing service is available
    And the authenticated user has publish permissions for topic "bjs.oes.event.dispatcher.inbound"

  # ============================================================================
  # POSITIVE SCENARIOS - Successful Event Publishing
  # ============================================================================

  @Positive @Smoke
  Scenario: Successfully publish a valid event to Event Store
    Given a valid event payload with unique event identifier
    And the event contains required metadata fields
    When the event is published to the Event Store
    Then the publish operation should complete successfully
    And an event identifier should be returned in the response

  @Positive
  Scenario: Publish event with custom headers preserved
    Given a valid event payload with unique event identifier
    And the event includes custom headers:
      | header-name          | header-value           |
      | x-correlation-id     | {{generated-uuid}}     |
      | x-source-system      | upstream-service       |
      | x-business-domain    | order-management       |
    When the event is published to the Event Store
    Then the publish operation should complete successfully
    And the custom headers should be acknowledged in the response

  @Positive
  Scenario: Publish event with complete metadata fields
    Given a valid event payload with unique event identifier
    And the event metadata includes:
      | field           | value                              |
      | eventType       | OrderCreated                       |
      | eventVersion    | 1.0.0                              |
      | sourceSystem    | order-service                      |
      | correlationId   | {{generated-uuid}}                 |
      | timestamp       | {{current-iso-timestamp}}          |
    When the event is published to the Event Store
    Then the publish operation should complete successfully
    And the metadata should be accepted without modification

  @Positive
  Scenario Outline: Publish events of various supported types
    Given a valid event payload with unique event identifier
    And the event type is "<eventType>"
    And the event version is "<eventVersion>"
    When the event is published to the Event Store
    Then the publish operation should complete successfully

    Examples:
      | eventType              | eventVersion |
      | OrderCreated           | 1.0.0        |
      | OrderUpdated           | 1.0.0        |
      | OrderCancelled         | 1.0.0        |
      | PaymentProcessed       | 2.0.0        |
      | InventoryReserved      | 1.1.0        |
      | ShipmentDispatched     | 1.0.0        |

  @Positive
  Scenario: Publish event with JSON payload containing nested structures
    Given a valid event payload with unique event identifier
    And the event payload contains nested JSON structures
    And the payload includes arrays and complex objects
    When the event is published to the Event Store
    Then the publish operation should complete successfully
    And the nested structure should be preserved

  @Positive
  Scenario: Publish event with idempotency key for deduplication
    Given a valid event payload with unique event identifier
    And an idempotency key is provided for the publish request
    When the event is published to the Event Store
    Then the publish operation should complete successfully
    And the idempotency key should be associated with the event

  # ============================================================================
  # NEGATIVE SCENARIOS - Authentication and Authorization Failures
  # ============================================================================

  @Negative @Security
  Scenario: Reject publish request without authentication credentials
    Given a valid event payload with unique event identifier
    But no authentication credentials are provided
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate unauthorized access
    And the HTTP status should indicate authentication required

  @Negative @Security
  Scenario: Reject publish request with expired authentication token
    Given a valid event payload with unique event identifier
    And the authentication token has expired
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate unauthorized access

  @Negative @Security
  Scenario: Reject publish request with invalid authentication token
    Given a valid event payload with unique event identifier
    And the authentication token is malformed
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate unauthorized access

  @Negative @Security
  Scenario: Reject publish request when user lacks publish permissions
    Given a valid event payload with unique event identifier
    And the authenticated user does not have publish permissions
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate forbidden access
    And the HTTP status should indicate authorization denied

  @Negative @Security
  Scenario: Reject publish request for unauthorized topic
    Given a valid event payload with unique event identifier
    And the target topic is "restricted.topic.unauthorized"
    And the authenticated user lacks permissions for this topic
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate forbidden access

  # ============================================================================
  # NEGATIVE SCENARIOS - Invalid Payload Validation
  # ============================================================================

  @Negative @Validation
  Scenario: Reject publish request with empty payload
    Given an empty event payload
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate invalid request
    And the validation error should specify payload is required

  @Negative @Validation
  Scenario: Reject publish request with malformed JSON payload
    Given an event payload with malformed JSON syntax
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate invalid request
    And the validation error should specify JSON parsing failure

  @Negative @Validation
  Scenario: Reject publish request missing required event identifier
    Given an event payload without event identifier
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate invalid request
    And the validation error should specify event identifier is required

  @Negative @Validation
  Scenario: Reject publish request missing required metadata
    Given an event payload with unique event identifier
    But the event metadata is missing required fields
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate invalid request
    And the validation error should specify missing metadata fields

  @Negative @Validation
  Scenario Outline: Reject publish request with invalid metadata field values
    Given an event payload with unique event identifier
    And the metadata field "<field>" has invalid value "<invalidValue>"
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate invalid request
    And the validation error should reference field "<field>"

    Examples:
      | field         | invalidValue                         |
      | eventType     |                                      |
      | eventType     | Invalid@Type#Characters              |
      | eventVersion  | not-a-version                        |
      | eventVersion  | 999.999.999                          |
      | timestamp     | not-a-timestamp                      |
      | timestamp     | 2025-13-45T99:99:99Z                 |

  @Negative @Validation
  Scenario: Reject publish request with unsupported content type
    Given a valid event payload with unique event identifier
    And the content type is set to "text/plain"
    When the event publish is attempted to the Event Store
    Then the publish operation should be rejected
    And the rejection reason should indicate unsupported media type

  # ============================================================================
  # NEGATIVE SCENARIOS - Infrastructure Failures
  # ============================================================================

  @Negative @Infrastructure
  Scenario: Handle downstream service unavailability during publish
    Given a valid event payload with unique event identifier
    And the downstream persistence layer is unavailable
    When the event publish is attempted to the Event Store
    Then the publish operation should fail with infrastructure error
    And the error response should indicate service unavailable
    And the error should be retriable

  @Negative @Infrastructure
  Scenario: Handle Kafka broker connection failure during publish
    Given a valid event payload with unique event identifier
    And the Kafka broker connection is disrupted
    When the event publish is attempted to the Event Store
    Then the publish operation should fail with infrastructure error
    And the error response should indicate processing failure

  @Negative @Infrastructure
  Scenario: Handle timeout during event serialization
    Given a valid event payload with unique event identifier
    And the serialization process encounters an error
    When the event publish is attempted to the Event Store
    Then the publish operation should fail with processing error
    And the error response should indicate unprocessable entity

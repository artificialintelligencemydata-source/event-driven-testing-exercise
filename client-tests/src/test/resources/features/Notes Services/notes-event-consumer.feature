#@notes @event-driven @kafka
#Feature: System-generated notes from Kafka events
#
#  Scenario: Persist note from inbound Kafka event
#    Given a valid event is published to "notes.inbound" topic
#    When the Notes event consumer processes the event
#    Then a system-generated note is created
#    And the note contains originEventId and originEventType
#    And the note is persisted exactly once
#    And processing latency is under 100ms
#
#  @edge
#  Scenario: Duplicate Kafka event received
#    Given a Kafka event with same idempotencyKey is reprocessed
#    When the consumer processes the event
#    Then no duplicate note is created
#
#  @failure
#  Scenario: Malformed Kafka event payload
#    Given an invalid event schema is received
#    When the consumer processes the event
#    Then the event is routed to DLQ
#    And an error log is generated

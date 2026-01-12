#@notes @resilience
#Feature: Error handling and recovery
#
#  Scenario: Document DB is unavailable during note creation
#    Given Document DB is down
#    When the user creates a note
#    Then the service retries with backoff
#    And returns status 503 after retries are exhausted
#
#  Scenario: Service restarts during event processing
#    Given the service crashes mid-event processing
#    When the service restarts
#    Then events are replayed from Kafka
#    And no duplicate notes are created

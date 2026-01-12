#@notes @immutability
#Feature: Notes are immutable
#
#  Scenario: Attempt to update an existing note
#    Given a note already exists
#    When the user attempts to update the note
#    Then the service returns status 405
#    And the note remains unchanged in Document DB
#
#  Scenario: Attempt to delete an existing note
#    Given a note already exists
#    When the user attempts to delete the note
#    Then the service returns status 405
#    And the note remains persisted

#@notes @search @regression
#Feature: Search Notes - GET /api/v1/notes
#
#  @smoke
#  Scenario Template: Search notes using valid filters "<TestCaseID>"
#    Given the user is authenticated
#    And notes exist matching:
#      | orderId  | noteType |
#      | <Order>  | SYSTEM   |
#    When the user searches notes with filters
#    Then the service returns status 200
#    And only matching notes are returned
#    And pagination metadata is present
#
#    Examples:
#      | TestCaseID | Order     |
#      | TC_SN_01   | ORD-1001  |
#      | TC_SN_02   | ORD-2002  |
#
#  @negative
#  Scenario: Invalid time range filter
#    Given the user is authenticated
#    And startTime is greater than endTime
#    When the user searches notes
#    Then the service returns status 400
#    And error indicates invalid query parameters
#
#  @security
#  Scenario: Forbidden role attempts note search
#    Given the user is authenticated with insufficient privileges
#    When the user searches notes
#    Then the service returns status 403

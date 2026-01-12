#@notes @retrieve @regression
#Feature: Retrieve Notes by Order ID - GET /api/v1/notes/order/{orderId}
#
#  @smoke
#  Scenario: Retrieve full note history for an order
#    Given multiple notes exist for orderId "ORD-1001"
#    When the user sends GET request to "/api/v1/notes/order/ORD-1001"
#    Then the service returns status 200
#    And all notes belong to orderId "ORD-1001"
#    And notes are sorted by createdAt descending
#    And retrieval is logged with correlationId
#
#  @edge
#  Scenario: Order exists but has no notes
#    Given no notes exist for orderId "ORD-9999"
#    When the user sends GET request to "/api/v1/notes/order/ORD-9999"
#    Then the service returns status 404
#    And error message indicates no notes found
#
#  @security
#  Scenario: Unauthorized access to retrieve notes
#    Given the user is not authenticated
#    When the user sends GET request to "/api/v1/notes/order/ORD-1001"
#    Then the service returns status 401

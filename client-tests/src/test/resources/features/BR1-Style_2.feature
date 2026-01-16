@bjs @oes @bopic @business-scenarios @event-driven
Feature: BOPIC - WCS Cart Submission to OES (Event Driven Validation)
  As a QA Engineer validating BJ's Order Event Service (OES)
  I want to validate routing, lifecycle, and asynchronous event sequence with ACK validations
  Using mocked WCS cart payload submission into Kafka
  So that order capture and processing are correct

  Background:
    Given OES is operational
    And all event consumers are registered
    And event correlation tracking is enabled
    And routing flags are set:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | Y     |
      | X-LIFECYCLE-IN-OES | Y     |

  # ==================================================================================
  # SC_BOPIC_001: HAPPY PATH - SINGLE LINE
  # ==================================================================================

  @SC_BOPIC_001 @single-line @happy-path
  Scenario Outline: Customer places a BOPIC single-line order from WCS - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the order number should be captured
    And the orderId is captured as "order_id"

    Then the order details can be retrieved using GET Order API
    And the retrieved order details should match the submitted cart payload

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated
    And the "PAYMENT_SAVED.causationId" must equal "init_id"
    And the "PAYMENT_SAVED.eventId" is captured as "payment_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated
    And the "FRAUD_RESPONSE_SUCCESS.causationId" must equal "payment_id"
    And the "FRAUD_RESPONSE_SUCCESS.eventId" is captured as "fraud_id"

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated
    And the "ORDER_CREATED.causationId" must equal "fraud_id"
    And the "ORDER_CREATED" event must occur only after both "PAYMENT_SAVED" and "FRAUD_RESPONSE_SUCCESS" ?????

    And the "SLOT CONFIRM" ACK should be generated

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    Then the "MIGRATION_INITIATED" event should be generated
    And the "MIGRATION_INITIATED" ACK should be generated

    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    # BR1 placeholders - do not fail if these do not exist in this release
    Then "ORDER_SCHEDULED" is marked as a BR1 placeholder milestone
    Then "ORDER_RELEASED" is marked as a BR1 placeholder milestone

#    to publish to Topic  - No access for QA3 - Darshan asked to using API
#    to consume to Topic  - To Consume message i need access for the topic
#    API CURD Operation   - Token is blocking








    Examples:
      | testcaseName              | TopicName             | Key |
      | BOPIC_SINGLELINE_REGULAR  | wcs.cart.submit.topic | 1   |
      | BOPIC_SINGLELINE_GROCERY  | wcs.cart.submit.topic | 2   |
      | BOPIC_SINGLELINE_WEIGHTED | wcs.cart.submit.topic | 3   |
      | BOPIC_SINGLELINE_AGE_21   | wcs.cart.submit.topic | 4   |
      | BOPIC_SINGLELINE_BULKY    | wcs.cart.submit.topic | 5   |

  # ==================================================================================
  # SC_BOPIC_002: HAPPY PATH - MULTI LINE
  # ==================================================================================

  @SC_BOPIC_002 @multi-line @happy-path
  Scenario Outline: Customer places a BOPIC multi-line order from WCS - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated
    And the "PAYMENT_SAVED.causationId" must equal "init_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | testcaseName            | TopicName             | Key |
      | BOPIC_MULTILINE_2LINES  | wcs.cart.submit.topic | 10  |
      | BOPIC_MULTILINE_3LINES  | wcs.cart.submit.topic | 11  |

  # ==================================================================================
  # SC_BOPIC_003: NEGATIVE - PAYMENT FAILED -> ORDER CANCELLED
  # ==================================================================================

  @SC_BOPIC_003 @single-line @negative @payment-failure
  Scenario Outline: Customer payment fails and order is cancelled - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the "PAYMENT_FAILED" event should be generated
    And the "PAYMENT_FAILED" ACK should be generated
    And the "PAYMENT_FAILED.causationId" must equal "init_id"

    Then the "ORDER_CANCELLED" event should be generated
    And the "ORDER_CANCELLED" ACK should be generated
    And the "ORDER_CANCELLED" event must occur after "PAYMENT_FAILED"

    And the order reaches terminal state "CANCELLED"

    Examples:
      | testcaseName                 | TopicName             | Key |
      | BOPIC_PAYMENT_FAILED_CANCEL  | wcs.cart.submit.topic | 101 |

      # ==================================================================================
  # SC_BOPIC_002: HAPPY PATH - MULTI LINE
  # ==================================================================================

  @SC_BOPIC_002 @multi-line @happy-path
  Scenario Outline: Customer places a BOPIC multi-line order from WCS - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated
    And the "PAYMENT_SAVED.causationId" must equal "init_id"
    And the "PAYMENT_SAVED.eventId" is captured as "payment_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated
    And the "FRAUD_RESPONSE_SUCCESS.causationId" must equal "payment_id"
    And the "FRAUD_RESPONSE_SUCCESS.eventId" is captured as "fraud_id"

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated
    And the "ORDER_CREATED.causationId" must equal "fraud_id"

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    # BR1 placeholders - do not fail if these do not exist in this release
    Then "ORDER_SCHEDULED" is marked as a BR1 placeholder milestone
    Then "ORDER_RELEASED" is marked as a BR1 placeholder milestone

    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | testcaseName            | TopicName             | Key |
      | BOPIC_MULTILINE_2LINES  | wcs.cart.submit.topic | 10  |
      | BOPIC_MULTILINE_3LINES  | wcs.cart.submit.topic | 11  |
      | BOPIC_MULTILINE_MIXQTY  | wcs.cart.submit.topic | 12  |


  # ==================================================================================
  # SC_BOPIC_006: SERVICE LINE / PICKUP FEE
  # ==================================================================================

  @SC_BOPIC_006 @service-line @pickup-fee @happy-path
  Scenario Outline: Customer places a BOPIC order with service line variations - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated
    And the "PAYMENT_SAVED.causationId" must equal "init_id"
    And the "PAYMENT_SAVED.eventId" is captured as "payment_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated
    And the "FRAUD_RESPONSE_SUCCESS.causationId" must equal "payment_id"

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | testcaseName                 | TopicName             | Key |
      | BOPIC_SERVICE_LINE_NO_FEE    | wcs.cart.submit.topic | 20  |
      | BOPIC_SERVICE_LINE_WITH_FEE  | wcs.cart.submit.topic | 21  |


  # ==================================================================================
  # SC_BOPIC_009-011: PAYMENT METHOD VARIANTS
  # Coverage: CreditCard, DebitCard, GiftCard, PayPal, Wallet
  # ==================================================================================

  @SC_BOPIC_009 @SC_BOPIC_010 @SC_BOPIC_011 @payment-methods @happy-path
  Scenario Outline: Customer places a BOPIC order using "<paymentMethod>" payment - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated
    And the "PAYMENT_SAVED.causationId" must equal "init_id"
    And the "PAYMENT_SAVED.eventId" is captured as "payment_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated
    And the "FRAUD_RESPONSE_SUCCESS.causationId" must equal "payment_id"

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | paymentMethod | testcaseName              | TopicName             | Key |
      | CreditCard    | BOPIC_PAY_CREDITCARD      | wcs.cart.submit.topic | 30  |
      | DebitCard     | BOPIC_PAY_DEBITCARD       | wcs.cart.submit.topic | 31  |
      | GiftCard      | BOPIC_PAY_GIFTCARD        | wcs.cart.submit.topic | 32  |
      | PayPal        | BOPIC_PAY_PAYPAL          | wcs.cart.submit.topic | 33  |
      | Wallet        | BOPIC_PAY_WALLET          | wcs.cart.submit.topic | 34  |


      # ==================================================================================
  # SC_BOPIC_003: HAPPY PATH - MIXED ITEM TYPES
  # Coverage: Mixed Regular + Grocery + Weighted + Bulky combinations
  # ==================================================================================

  @SC_BOPIC_003 @mixed-items @happy-path
  Scenario Outline: Customer places a BOPIC mixed-item order from WCS - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the order details can be retrieved using GET Order API
    And the retrieved order details should match the submitted cart payload

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated
    And the "PAYMENT_SAVED.causationId" must equal "init_id"
    And the "PAYMENT_SAVED.eventId" is captured as "payment_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated
    And the "FRAUD_RESPONSE_SUCCESS.causationId" must equal "payment_id"
    And the "FRAUD_RESPONSE_SUCCESS.eventId" is captured as "fraud_id"

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated
    And the "ORDER_CREATED.causationId" must equal "fraud_id"

    And the "ORDER_CREATED" event must occur only after both "PAYMENT_SAVED" and "FRAUD_RESPONSE_SUCCESS"

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    # BR1 placeholders - do not fail if these do not exist in this release
    Then "ORDER_SCHEDULED" is marked as a BR1 placeholder milestone
    Then "ORDER_RELEASED" is marked as a BR1 placeholder milestone

    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | testcaseName                       | TopicName             | Key |
      | BOPIC_MIXEDITEMS_REG_GROC          | wcs.cart.submit.topic | 60  |
      | BOPIC_MIXEDITEMS_REG_WEIGHTED      | wcs.cart.submit.topic | 61  |
      | BOPIC_MIXEDITEMS_REG_BULKY         | wcs.cart.submit.topic | 62  |
      | BOPIC_MIXEDITEMS_GROC_WEIGHTED     | wcs.cart.submit.topic | 63  |
      | BOPIC_MIXEDITEMS_FULL_COMBINATION  | wcs.cart.submit.topic | 64  |


  # ==================================================================================
  # SC_BOPIC_004: HAPPY PATH - HOT FOOD ITEMS
  # Coverage: Hot food items may have special routing/eligibility rules
  # ==================================================================================

  @SC_BOPIC_004 @hot-food @happy-path
  Scenario Outline: Customer places a BOPIC order containing hot food items - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the order details can be retrieved using GET Order API
    And the retrieved order details should match the submitted cart payload

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated
    And the "PAYMENT_SAVED.causationId" must equal "init_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | testcaseName                 | TopicName             | Key |
      | BOPIC_HOTFOOD_SINGLE_LINE    | wcs.cart.submit.topic | 70  |
      | BOPIC_HOTFOOD_MULTI_LINE     | wcs.cart.submit.topic | 71  |


  # ==================================================================================
  # NEGATIVE: PAYMENT_SAVED FAILED -> ORDER_CANCELLED
  # ==================================================================================

  @SC_BOPIC_005 @negative @payment-failure @order-cancelled
  Scenario Outline: Customer payment fails and order is cancelled - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated
    And the "ORDER_INITIATED.eventId" is captured as "init_id"

    Then the orderId is captured as "order_id"

    Then the "PAYMENT_FAILED" event should be generated
    And the "PAYMENT_FAILED" ACK should be generated
    And the "PAYMENT_FAILED.causationId" must equal "init_id"

    Then the "ORDER_CANCELLED" event should be generated
    And the "ORDER_CANCELLED" ACK should be generated
    And the "ORDER_CANCELLED" event must occur after "PAYMENT_FAILED"

    And the order reaches terminal state "CANCELLED"

    Examples:
      | testcaseName                 | TopicName             | Key |
      | BOPIC_PAYMENT_FAILED_CANCEL  | wcs.cart.submit.topic | 101 |


      # ==================================================================================
  # SC_BOPIC_007: ROUTING FLAG VARIATION - X-ROUTE-TO-OES = N
  # Expected: Order should NOT be routed to OES, OES lifecycle events must NOT be generated
  # ==================================================================================

  @SC_BOPIC_007 @routing @negative @route-away-from-oes
  Scenario Outline: Customer submits a BOPIC order when routing flag disables OES - "<testcaseName>"
    Given OES routing flags are overridden for this scenario:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | N     |
      | X-LIFECYCLE-IN-OES | Y     |

    And the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order must NOT be routed to OES

    Then the "ORDER_INITIATED" event should NOT be generated in OES
    And the "ORDER_INITIATED" ACK should NOT be generated in OES

    Then the "PAYMENT_SAVED" event should NOT be generated in OES
    And the "PAYMENT_SAVED" ACK should NOT be generated in OES

    Then the "ORDER_CREATED" event should NOT be generated in OES
    And the "ORDER_CREATED" ACK should NOT be generated in OES

    Examples:
      | testcaseName             | TopicName             | Key |
      | BOPIC_SINGLELINE_REGULAR | wcs.cart.submit.topic | 200 |


  # ==================================================================================
  # SC_BOPIC_008: LIFECYCLE FLAG VARIATION - X-LIFECYCLE-IN-OES = N
  # Expected: Order may route to OES, but lifecycle events should NOT be managed in OES
  # ==================================================================================

  @SC_BOPIC_008 @lifecycle @negative @lifecycle-outside-oes
  Scenario Outline: Customer submits a BOPIC order when lifecycle flag disables OES lifecycle - "<testcaseName>"
    Given OES routing flags are overridden for this scenario:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | Y     |
      | X-LIFECYCLE-IN-OES | N     |

    And the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags

    # Depending on architecture, order may be captured but lifecycle events should not proceed in OES
    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated

    Then the "PAYMENT_SAVED" event should NOT be generated in OES
    And the "PAYMENT_SAVED" ACK should NOT be generated in OES

    Then the "FRAUD_RESPONSE_SUCCESS" event should NOT be generated in OES
    And the "FRAUD_RESPONSE_SUCCESS" ACK should NOT be generated in OES

    Then the "ORDER_CREATED" event should NOT be generated in OES
    And the "ORDER_CREATED" ACK should NOT be generated in OES

    Examples:
      | testcaseName             | TopicName             | Key |
      | BOPIC_SINGLELINE_REGULAR | wcs.cart.submit.topic | 201 |


  # ==================================================================================
  # SC_BOPIC_012: ROUTING + LIFECYCLE BOTH OFF
  # Expected: No OES processing at all
  # ==================================================================================

  @SC_BOPIC_012 @routing @lifecycle @negative @oes-disabled
  Scenario Outline: Customer submits a BOPIC order when both OES routing and lifecycle are disabled - "<testcaseName>"
    Given OES routing flags are overridden for this scenario:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | N     |
      | X-LIFECYCLE-IN-OES | N     |

    And the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order must NOT be routed to OES

    Then the "ORDER_INITIATED" event should NOT be generated in OES
    And the "ORDER_INITIATED" ACK should NOT be generated in OES

    Then the "PAYMENT_SAVED" event should NOT be generated in OES
    And the "PAYMENT_SAVED" ACK should NOT be generated in OES

    Then the "ORDER_CREATED" event should NOT be generated in OES
    And the "ORDER_CREATED" ACK should NOT be generated in OES

    Examples:
      | testcaseName             | TopicName             | Key |
      | BOPIC_SINGLELINE_REGULAR | wcs.cart.submit.topic | 202 |


      # ==================================================================================
  # SC_BOPIC_013: MIGRATION INITIATED OPTIONAL, MIGRATION COMPLETE MANDATORY
  # ==================================================================================

  @SC_BOPIC_013 @migration @happy-path
  Scenario Outline: Customer BOPIC order completes migration even if migration initiated is optional - "<testcaseName>"
    Given the customer loads cart test data "<testcaseName>"
    And the cart payload is prepared for publishing

    When the customer submits the cart payload to Kafka for order capture:
      | Field | Value       |
      | topic | <TopicName> |
      | key   | <Key>       |

    Then the cart request is accepted for processing
    And the order is routed to OES based on routing flags
    And the order lifecycle continues in OES based on lifecycle flags

    Then the "ORDER_INITIATED" event should be generated
    And the "ORDER_INITIATED" ACK should be generated

    Then the orderId is captured as "order_id"

    Then the "PAYMENT_SAVED" event should be generated
    And the "PAYMENT_SAVED" ACK should be generated

    Then the "FRAUD_RESPONSE_SUCCESS" event should be generated
    And the "FRAUD_RESPONSE_SUCCESS" ACK should be generated

    Then the "ORDER_CREATED" event should be generated
    And the "ORDER_CREATED" ACK should be generated

    Then the "HOLD_PROCESS_COMPLETE" event should be generated
    And the "HOLD_PROCESS_COMPLETE" ACK should be generated

    # Migration initiated is optional per routing flow
    Then the "MIGRATION_INITIATED" event should be generated if emitted
    And the "MIGRATION_INITIATED" ACK should be generated if emitted

    # Migration complete must always be emitted for successful order migration
    Then the "MIGRATION_COMPLETE" event should be generated
    And the "MIGRATION_COMPLETE" ACK should be generated

    And the order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | testcaseName              | TopicName             | Key |
      | BOPIC_SINGLELINE_REGULAR  | wcs.cart.submit.topic | 300 |
      | BOPIC_MULTILINE_2LINES    | wcs.cart.submit.topic | 301 |

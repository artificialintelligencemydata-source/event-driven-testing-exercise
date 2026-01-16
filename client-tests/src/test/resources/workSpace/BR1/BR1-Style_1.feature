@oes @event-driven @canonical-lifecycle
Feature: OES Event-Driven Order Lifecycle - Canonical Flows
  As a QA Engineer
  I want to validate all canonical OES order lifecycle flows
  Using corrected event sequences and proper terminal state validation
  So that business-observable order processing is verified correctly

  Background:
    Given the OES system is operational
    And all event consumers are registered
    And event correlation and causation tracking is enabled

  # ==================================================================================
  # CANONICAL HAPPY PATH: OES LIFECYCLE (X-LIFECYCLE-IN-OES = Y)
  # ==================================================================================

  @happy-path @oes-lifecycle @stay-in-oes @canonical
  Scenario: Complete OES lifecycle with order staying in OES - Canonical Flow
    Given I am placing an order for product "XXXX" with quantity "1"
    And routing flags are configured as:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | Y     |
      | X-LIFECYCLE-IN-OES | Y     |

    # EVENT 1: ORDER_INITIATED (First Event)
    When I submit the order
    Then I verify "ORDER_INITIATED" event is published within 5 seconds
    And I validate ORDER_INITIATED event contains:
      | Field         | Value                     |
      | sourceSystem  | order-process-service     |
      | eventType     | order.initiated           |
      | eventSubType  | order.initiated.success   |
      | reason        | ORDER_REQUEST_VALIDATED   |
    And I verify order state transitions to "INITIATED"
    And I capture ORDER_INITIATED.eventId as "causationId_1"

    # EVENT 2: PAYMENT_SAVED
    Then I verify "PAYMENT_SAVED" event is published within 8 seconds
    And I validate PAYMENT_SAVED event contains:
      | Field         | Value                   |
      | sourceSystem  | payment-service         |
      | eventType     | payment.saved           |
      | eventSubType  | payment.saved.success   |
      | reason        | PAYMENT_AUTH_SUCCESS    |
    And I verify PAYMENT_SAVED.causationId equals ORDER_INITIATED.eventId
    And I verify payment state is "SAVED"
    And I verify payment payload includes:
      | Field              | Required |
      | paymentStatus      | Yes      |
      | paymentMethods     | Yes      |
      | totalAuthorized    | Yes      |
    And I capture PAYMENT_SAVED.eventId as "causationId_2"

    # EVENT 3: FRAUD_RESPONSE_SUCCESS
    Then I verify "FRAUD_RESPONSE_SUCCESS" event is published within 12 seconds
    And I validate FRAUD_RESPONSE_SUCCESS event contains:
      | Field         | Value                       |
      | sourceSystem  | payment-event-service       |
      | eventType     | fraud.response              |
      | eventSubType  | fraud.response.success      |
    And I verify FRAUD_RESPONSE_SUCCESS.causationId equals PAYMENT_SAVED.eventId
    And I verify fraud status is "PASS"
    And I capture FRAUD_RESPONSE_SUCCESS.eventId as "causationId_3"

    # EVENT 4: ORDER_CREATED (After BOTH payment AND fraud success)
    Then I verify "ORDER_CREATED" event is published within 5 seconds
    And I validate ORDER_CREATED event contains:
      | Field         | Value                          |
      | sourceSystem  | order-process-service          |
      | eventType     | order.created                  |
      | eventSubType  | order.created.success          |
      | reason        | ORDER_SUCCESSFULLY_CREATED     |
    And I verify ORDER_CREATED.causationId equals FRAUD_RESPONSE_SUCCESS.eventId
    And I verify order state transitions to "CREATED"
    And I verify success acknowledgment is sent to OCS
    And I capture ORDER_CREATED.eventId as "causationId_4"

    # IMPLICIT GATE: Hold processing (not a public event)
    Then I verify hold processing completes

    # LIFECYCLE DECISION: Stay in OES
    When lifecycle decision is evaluated
    Then I verify X-LIFECYCLE-IN-OES flag is "Y"
    And I verify order stays in OES
    And I verify no migration events are published

    # EVENT 5: SLOT_CONFIRMATION (Last Documented Event)
    Then I verify "SLOT_CONFIRMATION" event is published within 10 seconds
    And I validate SLOT_CONFIRMATION event contains:
      | Field         | Value                     |
      | sourceSystem  | schedule-event-service    |
      | eventType     | SLOT_CONFIRMATION         |
    And I verify SLOT_CONFIRMATION payload includes:
      | Field         | Required |
      | fulfillments  | Yes      |
      | orderLines    | Yes      |
      | slot          | Yes      |
    And I verify slot is confirmed

    # TERMINAL STATE VALIDATION
    Then I verify order reaches terminal state "OES_LIFECYCLE_COMPLETE"
    And I verify last documented event is "SLOT_CONFIRMATION"
    And I verify no further events are published after terminal state
    And I verify all events share same correlationId
    And I verify causation chain is complete:
      | Event                    | Caused By                    |
      | PAYMENT_SAVED           | ORDER_INITIATED              |
      | FRAUD_RESPONSE_SUCCESS  | PAYMENT_SAVED                |
      | ORDER_CREATED           | FRAUD_RESPONSE_SUCCESS       |
    And I verify terminal state means "completion of documented OES lifecycle events"

  # ==================================================================================
  # CANONICAL MIGRATION PATH (X-LIFECYCLE-IN-OES = N)
  # ==================================================================================

  @migration-path @oes-to-sterling @canonical
  Scenario: Order migration from OES to Sterling - Canonical Flow
    Given I am placing an order for product "LAPTOP002" with quantity "2"
    And routing flags are configured as:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | Y     |
      | X-LIFECYCLE-IN-OES | N     |

    # EVENTS 1-4: Same as Happy Path
    When I submit the order
    Then I verify canonical event sequence through ORDER_CREATED:
      | Sequence | Event                    | Source System           |
      | 1        | ORDER_INITIATED          | order-process-service   |
      | 2        | PAYMENT_SAVED            | payment-service         |
      | 3        | FRAUD_RESPONSE_SUCCESS   | payment-event-service   |
      | 4        | ORDER_CREATED            | order-process-service   |
    And I verify order state is "CREATED"

    # IMPLICIT GATE: Hold processing
    Then I verify hold processing completes

    # LIFECYCLE DECISION: Migrate to Sterling
    When lifecycle decision is evaluated
    Then I verify X-LIFECYCLE-IN-OES flag is "N"
    And I verify migration workflow is triggered

    # EVENT 5: MIGRATION_INITIATED
    Then I verify "MIGRATION_INITIATED" event is published within 10 seconds
    And I validate MIGRATION_INITIATED event contains:
      | Field         | Value                        |
      | sourceSystem  | order-process-service        |
      | eventType     | order.migration              |
      | eventSubType  | order.migration.initiated    |
      | reason        | ORDER_MIGRATION_INITIATED    |
    And I verify order status updates to "Migration In Progress"
    And I verify order source updates to "MIGRATION_INITIATED"

    # PAYLOAD TRANSFORMATION (not event-observable)
    Then I verify Order Router transforms OES payload to Sterling format

    # EVENT 6: MIGRATION_COMPLETED (Terminal Event)
    Then I verify "MIGRATION_COMPLETED" event is published within 15 seconds
    And I validate MIGRATION_COMPLETED event contains:
      | Field         | Value                        |
      | sourceSystem  | order-process-service        |
      | eventType     | order.migration              |
      | eventSubType  | order.migration.completed    |
      | reason        | ORDER_MIGRATION_COMPLETED    |
    And I verify order status updates to "Migrated"
    And I verify order source updates to "STERLING"
    And I verify downstream systems receive MIGRATION_COMPLETED event
    And I verify purge timers are activated

    # TERMINAL STATE VALIDATION
    Then I verify order reaches terminal state "MIGRATED_TO_STERLING"
    And I verify no further OES events are published
    And I verify migration is one-way (no reverse migration)
    And I verify Sterling now owns the order

  # ==================================================================================
  # DIRECT STERLING ROUTING (X-ROUTE-TO-OES = N)
  # ==================================================================================

  @direct-routing @bypass-oes @first-level-filter
  Scenario: Order routes directly to Sterling bypassing OES
    Given I am placing an order for special product "STH_ITEM001"
    And routing flags are configured as:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | N     |

    When I submit the order
    Then I verify routing decision is "Route to Sterling"
    And I verify order bypasses OES completely
    And I verify NO OES events are published:
      | Event               | Should NOT Publish |
      | ORDER_INITIATED     | ✗                  |
      | PAYMENT_SAVED       | ✗                  |
      | ORDER_CREATED       | ✗                  |
      | MIGRATION_INITIATED | ✗                  |
    And I verify order is sent directly to Sterling
    And I verify order reaches terminal state "IN_STERLING"

  # ==================================================================================
  # FAILURE PATH 1: INITIATION FAILURE
  # ==================================================================================

  @failure-path @initiation-failure @validation-error
  Scenario: Order initiation fails due to validation errors
    Given I am placing an order with invalid data:
      | Field         | Value   |
      | billingInfo   | missing |
      | customerId    | invalid |

    When I submit the order
    Then I verify "ORDER_INITIATE_FAILED" event is published within 5 seconds
    And I validate ORDER_INITIATE_FAILED event contains:
      | Field         | Value                        |
      | sourceSystem  | order-processing-service     |
      | eventType     | order.initiate               |
      | eventSubType  | order.initiate.failed        |
      | reason        | ORDER_VALIDATION_FAILED      |
    And I validate error payload includes:
      | Field         | Present |
      | errorCode     | Yes     |
      | errorReason   | Yes     |
      | errorDetails  | Yes     |
    And I verify failure acknowledgment is sent to OCS
    And I verify order reaches terminal state "INITIATION_FAILED"
    And I verify NO subsequent events are published
    And I verify order is NOT created in database

  # ==================================================================================
  # FAILURE PATH 2: PAYMENT FAILURE (CORRECTED SEMANTICS)
  # ==================================================================================

  @failure-path @payment-failure @terminal-from-business
  Scenario: Payment authorization fails - Terminal from business perspective
    Given I am placing an order with payment that will be declined

    # ORDER_INITIATED succeeds
    When I submit the order
    Then I verify "ORDER_INITIATED" event is published
    And I verify order is validated and persisted
    And I verify order state is "INITIATED"

    # PAYMENT_SAVE_FAILED occurs (Terminal)
    When payment processing is attempted
    Then I verify "PAYMENT_SAVE_FAILED" event is published within 10 seconds
    And I validate PAYMENT_SAVE_FAILED event contains:
      | Field         | Value                  |
      | sourceSystem  | payment-service        |
      | eventType     | payment.save           |
      | eventSubType  | payment.save.failed    |
      | reason        | PAYMENT_AUTH_FAILURE   |
    And I validate error details include:
      | Field         | Present |
      | errorReason   | Yes     |
      | errorDetails  | Yes     |

    # TERMINAL STATE VALIDATION
    Then I verify order reaches terminal state "PAYMENT_FAILED"
    And I verify PAYMENT_SAVE_FAILED is terminal from business perspective
    And I verify any retries or recovery are implementation-level (out of scope)
    And I verify failure acknowledgment is sent

    # NOTE: ORDER_CREATE_FAILED may or may not follow (implementation-dependent)
    And I note that ORDER_CREATE_FAILED is optional and not guaranteed

  # ==================================================================================
  # FAILURE PATH 2b: PAYMENT FAILURE WITH ORDER_CREATE_FAILED
  # ==================================================================================

  @failure-path @payment-failure @creation-failed-branch
  Scenario: Payment failure followed by order creation failure (implementation-dependent)
    Given I am placing an order with payment that will be declined

    When I submit the order
    Then I verify "ORDER_INITIATED" event is published
    And I verify "PAYMENT_SAVE_FAILED" event is published

    # Implementation-dependent branch
    When ORDER_CREATE_FAILED is published
    Then I validate ORDER_CREATE_FAILED event contains:
      | Field         | Value                     |
      | sourceSystem  | order-processing-service  |
      | eventType     | order.created             |
      | eventSubType  | order.created.failed      |
      | reason        | ORDER_CREATION_FAILED     |
    And I verify order reaches terminal state "CREATION_FAILED"
    And I verify this is an implementation-dependent branch

  # ==================================================================================
  # FAILURE PATH 3: FRAUD FAILURE (CORRECTED SEMANTICS)
  # ==================================================================================

  @failure-path @fraud-failure @non-terminal-coordination
  Scenario: Order cancelled due to fraud check failure
    Given I am placing an order with high-risk indicators

    # ORDER_INITIATED and PAYMENT_SAVED succeed
    When I submit the order
    Then I verify "ORDER_INITIATED" event is published
    And I verify "PAYMENT_SAVED" event is published
    And I verify order state is "INITIATED"
    And I verify payment state is "SAVED"

    # FRAUD_RESPONSE_FAILURE occurs (Non-Terminal)
    When fraud check is performed
    Then I verify "FRAUD_RESPONSE_FAILURE" event is published within 12 seconds
    And I validate FRAUD_RESPONSE_FAILURE event contains:
      | Field         | Value                      |
      | sourceSystem  | payment-event-service      |
      | eventType     | fraud.response             |
      | eventSubType  | fraud.response.failure     |
    And I verify FRAUD_RESPONSE_FAILURE is non-terminal (coordination event)

    # ORDER_CANCELLED follows (Terminal)
    Then I verify "ORDER_CANCELLED" event is published within 5 seconds
    And I validate ORDER_CANCELLED event contains:
      | Field                | Value                         |
      | sourceSystem         | order-process-service         |
      | eventType            | order.cancelled               |
      | eventSubType         | order.cancelled.fraudResponse |
      | reason               | FRAUD_RESPONSE_FAILURE        |
      | fullOrderCancel      | true                          |
    And I validate cancellation payload includes:
      | Field                  | Required |
      | cancellationDetails    | Yes      |
      | orderLines             | Yes      |
      | overallTotals          | Yes      |
    And I verify all payment values are set to 0
    And I verify ORDER_CANCELLED is the terminal state

    # TERMINAL STATE VALIDATION
    Then I verify order reaches terminal state "CANCELLED_FRAUD"
    And I verify failure acknowledgment is sent
    And I verify no fulfillment occurs

  # ==================================================================================
  # EVENT HEADER VALIDATION
  # ==================================================================================

  @event-structure @headers @mandatory-fields
  Scenario: All events contain mandatory header fields
    Given an order is being processed through OES
    When any OES event is published
    Then I verify the event contains all mandatory headers:
      | Header Field        | Type     | Required |
      | eventId             | UUID     | Yes      |
      | idempotencyKey      | String   | Yes      |
      | correlationId       | UUID     | Yes      |
      | causationId         | UUID     | Yes      |
      | eventTime           | Timestamp| Yes      |
      | producerTime        | Timestamp| Yes      |
      | sourceSystem        | String   | Yes      |
      | eventType           | String   | Yes      |
      | eventSubType        | String   | Yes      |
      | forceReprocess      | Boolean  | Yes      |
      | maxRetryCount       | Integer  | Yes      |
      | actorType           | String   | Yes      |
      | actorId             | String   | Yes      |
      | triggerType         | String   | Yes      |
      | reason              | String   | Yes      |
      | orderNo             | String   | Yes      |
      | signature           | String   | Yes      |
      | signatureAlgorithm  | String   | Yes      |
      | payloadHash         | String   | Yes      |

  # ==================================================================================
  # CAUSATION CHAIN VALIDATION
  # ==================================================================================

  @causation-chain @event-correlation @traceability
  Scenario: Events maintain correct causation chain throughout lifecycle
    Given I am processing an order through complete OES lifecycle
    When all events are published
    Then I verify causation chain links are correct:
      | Event                    | Causation ID Source          |
      | ORDER_INITIATED          | Initial request              |
      | PAYMENT_SAVED            | ORDER_INITIATED.eventId      |
      | FRAUD_RESPONSE_SUCCESS   | PAYMENT_SAVED.eventId        |
      | ORDER_CREATED            | FRAUD_RESPONSE_SUCCESS.eventId |
      | SLOT_CONFIRMATION        | ORDER_CREATED.eventId        |
    And I verify each event's causationId equals previous event's eventId
    And I verify correlationId is identical across all events
    And I verify correlationId equals orderNo
    And I verify full event chain can be reconstructed from causation IDs

  # ==================================================================================
  # IDEMPOTENCY KEY VALIDATION
  # ==================================================================================

  @idempotency @duplicate-prevention @key-format
  Scenario: Idempotency keys prevent duplicate event processing
    Given an order "ORD-12345" is being processed
    When "ORDER_INITIATED" event is published
    Then I verify idempotency key follows format "hash_order.initiated.v1_ORD-12345_step1"
    And I verify key format is "hash_{eventType}_{orderNo}_{step}"
    When the same event is published again with same idempotency key
    Then I verify duplicate is detected
    And I verify event is not processed twice
    And I verify data consistency is maintained
    And I verify only one order record exists

  # ==================================================================================
  # NOTES FIELD VALIDATION
  # ==================================================================================

  @audit-trail @notes @user-tracking
  Scenario Outline: Events capture user actions correctly in notes field
    Given an order action is performed by "<ActorType>"
    When an event is published with notes
    Then I verify notes field contains:
      | Field     | Value for Contact Center | Value for Digital User |
      | userId    | Associate Login ID       | Member                 |
      | userName  | Associate Name           | Member                 |
    And I verify notes field structure includes:
      | Field      | Required |
      | type       | No       |
      | reason     | No       |
      | text       | No       |
      | entityKey  | No       |
      | userId     | Yes      |
      | userName   | Yes      |

    Examples:
      | ActorType        |
      | Contact Center   |
      | Digital User     |

  # ==================================================================================
  # ERROR EVENT STRUCTURE VALIDATION
  # ==================================================================================

  @error-events @failure-handling @error-structure
  Scenario: Failure events contain comprehensive error information
    Given an error occurs during order processing
    When a failure event is published
    Then I verify the event contains error fields in header:
      | Field         | Required |
      | errorCode     | Yes      |
      | errorReason   | Yes      |
      | errorDetails  | Yes      |
    And I verify payload contains errors array
    And I verify each error object in array contains:
      | Field         | Required |
      | errorCode     | Yes      |
      | errorReason   | Yes      |
      | errorDetails  | Yes      |

  # ==================================================================================
  # ROUTING FLAG VALIDATION
  # ==================================================================================

  @routing @flags @decision-logic
  Scenario Outline: Routing flags determine correct order path
    Given routing flags are configured as:
      | Flag Name           | Value              |
      | X-ROUTE-TO-OES     | <RouteToOES>       |
      | X-LIFECYCLE-IN-OES | <LifecycleInOES>   |
    When I submit an order
    Then I verify order follows path "<ExpectedPath>"
    And I verify terminal state is "<ExpectedTerminalState>"

    Examples:
      | RouteToOES | LifecycleInOES | ExpectedPath          | ExpectedTerminalState    |
      | Y          | Y              | OES Lifecycle         | OES_LIFECYCLE_COMPLETE   |
      | Y          | N              | OES → Sterling        | MIGRATED_TO_STERLING     |
      | N          | N/A            | Direct Sterling       | IN_STERLING              |

  # ==================================================================================
  # TERMINAL STATE VALIDATION
  # ==================================================================================

  @terminal-states @lifecycle-completion @final-validation
  Scenario Outline: Orders reach correct terminal states for each flow
    Given I am processing an order with scenario "<Scenario>"
    When the order flow completes
    Then I verify order reaches terminal state "<TerminalState>"
    And I verify terminal state is business-observable
    And I verify no further events are published after terminal state
    And I verify terminal state meaning matches "<Meaning>"

    Examples:
      | Scenario                  | TerminalState              | Meaning                                    |
      | Happy Path OES           | OES_LIFECYCLE_COMPLETE     | Completion of documented OES events        |
      | Migration Complete       | MIGRATED_TO_STERLING       | Order transferred to Sterling              |
      | Direct Sterling          | IN_STERLING                | Order bypassed OES                         |
      | Initiation Failed        | INITIATION_FAILED          | Order could not start                      |
      | Payment Failed           | PAYMENT_FAILED             | Payment not secured (terminal from business)|
      | Creation Failed          | CREATION_FAILED            | Order creation failed                      |
      | Fraud Cancelled          | CANCELLED_FRAUD            | Order stopped due to risk                  |

  # ==================================================================================
  # ORDER_CREATED TIMING VALIDATION (CRITICAL)
  # ==================================================================================

  @order-created @timing @critical-validation
  Scenario: ORDER_CREATED publishes only after BOTH payment AND fraud success
    Given I am placing an order
    When I submit the order
    Then I verify "ORDER_INITIATED" is published first
    And I verify "PAYMENT_SAVED" is published second
    And I verify "FRAUD_RESPONSE_SUCCESS" is published third
    And I verify "ORDER_CREATED" is published fourth
    And I verify ORDER_CREATED does NOT publish before:
      | Prerequisite Event       | Must Complete First |
      | PAYMENT_SAVED           | Yes                 |
      | FRAUD_RESPONSE_SUCCESS  | Yes                 |
    And I verify ORDER_CREATED meaning is "order successfully created after all validations"
    And I verify ORDER_CREATED is NOT the first event

  # ==================================================================================
  # SLOT_CONFIRMATION SEMANTICS VALIDATION
  # ==================================================================================

  @slot-confirmation @last-documented-event @lifecycle-completion
  Scenario: SLOT_CONFIRMATION is last documented coordination event
    Given an order completes OES lifecycle (X-LIFECYCLE-IN-OES = Y)
    When all documented events are published
    Then I verify "SLOT_CONFIRMATION" is the last documented event
    And I verify SLOT_CONFIRMATION is NOT a fulfillment indicator
    And I verify SLOT_CONFIRMATION is NOT a release terminal indicator
    And I verify OES_LIFECYCLE_COMPLETE means "completion of documented OES lifecycle events"
    And I verify terminal state does NOT mean fulfillment or delivery completion

  # ==================================================================================
  # PAYMENT_SAVED PAYLOAD VALIDATION
  # ==================================================================================

  @payment-saved @payload-validation @complex-payload
  Scenario: PAYMENT_SAVED event contains complete payment information
    Given payment is successfully processed
    When "PAYMENT_SAVED" event is published
    Then I validate payment payload contains:
      | Field              | Required |
      | orderNo            | Yes      |
      | orderHeaderKey     | Yes      |
      | paymentStatus      | Yes      |
      | paymentMethods     | Yes      |
    And I validate paymentMethods array contains:
      | Field                    | Required |
      | paymentType              | Yes      |
      | cardType                 | Yes      |
      | paymentIdentifier        | Yes      |
      | lastFourDigits           | Yes      |
      | chargeSequence           | Yes      |
      | maxChargeLimit           | Yes      |
      | transactionDetails       | Yes      |
      | totalAuthorized          | Yes      |
    And I verify transactionDetails include transactionId, amount, status

  # ==================================================================================
  # ORDER_CANCELLED PAYLOAD VALIDATION
  # ==================================================================================

  @order-cancelled @payload-validation @financial-reconciliation
  Scenario: ORDER_CANCELLED event contains complete financial breakdown
    Given an order is being cancelled due to fraud
    When "ORDER_CANCELLED" event is published
    Then I validate the payload contains:
      | Section                 | Required |
      | orderNo                 | Yes      |
      | fullOrderCancel         | Yes      |
      | cancellationDetails     | Yes      |
      | orderLines              | Yes      |
      | overallTotals           | Yes      |
    And I verify each order line contains:
      | Field                | Required |
      | primeLineNo          | Yes      |
      | orderedQuantity      | Yes      |
      | cancelledQuantity    | Yes      |
      | overallTotals        | Yes      |
      | taxes                | Yes      |
      | charges              | Yes      |
    And I verify when fullOrderCancel is true all payment values are 0

  # ==================================================================================
  # SLOT_CONFIRMATION PAYLOAD VALIDATION
  # ==================================================================================

  @slot-confirmation @payload-validation @fulfillment-details
  Scenario: SLOT_CONFIRMATION event contains complete fulfillment details
    Given delivery slot is being confirmed
    When "SLOT_CONFIRMATION" event is published
    Then I validate fulfillments array contains:
      | Field            | Required |
      | fulfillmentType  | Yes      |
      | vendor           | Yes      |
      | delivery         | Yes      |
      | slot             | Yes      |
    And I verify delivery object contains:
      | Field                  | Required |
      | deliveryType           | Yes      |
      | vendor                 | Yes      |
      | slotReservationId      | Yes      |
    And I verify slot object contains:
      | Field      | Required |
      | slotId     | Yes      |
      | startTime  | Yes      |
      | endTime    | Yes      |

  # ==================================================================================
  # UNDOCUMENTED EVENTS HANDLING
  # ==================================================================================

  @undocumented-events @specification-gap @graceful-handling
  Scenario: System handles undocumented events gracefully
    Given the following events exist but are undocumented:
      | Event Name           | Evidence         |
      | ORDERS_AUTHORIZED    | In diagrams      |
      | ORDER_SCHEDULED      | In diagrams      |
      | ORDER_RELEASED       | In diagrams      |
      | ORDER_INVOICED       | In diagrams      |
      | AUTHSETTLEMENT       | In diagrams      |
      | DEMAND_UPDATE        | In diagrams      |
      | FRAUD_CHECK_REQUEST  | Implied by flow  |
    When these events are encountered in the system
    Then I do NOT fail tests due to lack of payload specifications
    And I log that these events exist but are undocumented
    And I request payload specifications from development team
    And I continue testing documented events

  # ==================================================================================
  # HOLD_PROCSES_COMPLETE HANDLING
  # ==================================================================================

  @hold-processing @implicit-gate @lifecycle-checkpoint
  Scenario: HOLD_PROCSES_COMPLETE is treated as implicit lifecycle gate
    Given an order is progressing through OES lifecycle
    When hold processing occurs
    Then I do NOT test HOLD_PROCSES_COMPLETE as a public event
    And I verify lifecycle decision outcome instead
    And I note that spelling "HOLD_PROCSES_COMPLETE" is preserved from diagrams
    And I verify it acts as internal checkpoint before lifecycle decision
    And I verify it is NOT required for BDD/E2E unless explicitly consumed externally


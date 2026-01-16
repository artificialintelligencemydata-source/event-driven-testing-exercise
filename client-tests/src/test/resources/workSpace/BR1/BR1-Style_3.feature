@bjs @oes @bopic @business-scenarios @event-driven
Feature: BOPIC Business Scenarios - Event-Driven Order Lifecycle
  As a QA Engineer validating BJ's Order Event Service
  I want to verify end-to-end BOPIC order business behavior
  Using event-driven asynchronous validation
  So that order capture, payment, scheduling, and migration work correctly

  Background:
    Given the OES system is operational
    And all event consumers are registered
    And event correlation tracking is enabled
    And routing flags are configured with:
      | Flag Name           | Value |
      | X-ROUTE-TO-OES     | Y     |
      | X-LIFECYCLE-IN-OES | Y     |

  # ==================================================================================
  # SC_BOPIC_001: SINGLE-LINE ORDER SCENARIOS
  # Coverage: Regular, Grocery, Weighted, Age-Restricted, Bulky items
  # Each example runs as a separate scenario
  # ==================================================================================

  @SC_BOPIC_001 @single-line
  Scenario Outline: BOPIC Single-Line Order - <ItemType> Item with <PaymentType> Payment
    Given a cart payload from WCS with:
      | Field                  | Value              |
      | fulfillmentType        | BOPIC              |
      | channel                | <Channel>          |
      | membershipType         | <MembershipType>   |
      | lines                  | 1                  |
    And line 1 contains:
      | Field            | Value              |
      | itemSKU          | <ItemSKU>          |
      | itemType         | <ItemType>         |
      | quantity         | <Quantity>         |
      | unitPrice        | <UnitPrice>        |
      | pricePerPound    | <PricePerPound>    |
      | estimatedWeight  | <EstimatedWeight>  |
      | ageRestriction   | <AgeRestriction>   |
      | lineTotal        | <LineTotal>        |
    And payment method is configured as:
      | Field            | Value              |
      | paymentType      | <PaymentType>      |
      | cardType         | <CardType>         |
    And pickup club is "<PickupClub>"
    And cart totals are:
      | Field              | Value           |
      | subtotal           | <Subtotal>      |
      | tax                | <Tax>           |
      | pickupFee          | <PickupFee>     |
      | grandTotal         | <GrandTotal>    |

    When the order payload is routed to OES for processing

    Then the "ORDER_INITIATED" event should be observed
    And I capture ORDER_INITIATED.eventId as "init_id"
    And I validate ORDER_INITIATED payload contains:
      | Field           | Value                     |
      | sourceSystem    | order-process-service     |
      | eventSubType    | order.initiated.success   |

    Then the "PAYMENT_SAVED" event should be observed
    And I validate PAYMENT_SAVED.causationId equals "init_id"
    And I capture PAYMENT_SAVED.eventId as "payment_id"
    And I validate PAYMENT_SAVED payload contains:
      | Field               | Value           |
      | paymentStatus       | PAYMENT_SAVED   |
      | paymentType         | <PaymentType>   |
      | totalAuthorized     | <GrandTotal>    |

    Then the "FRAUD_RESPONSE_SUCCESS" event should be observed
    And I validate FRAUD_RESPONSE_SUCCESS.causationId equals "payment_id"
    And I capture FRAUD_RESPONSE_SUCCESS.eventId as "fraud_id"

    Then the "ORDER_CREATED" event should be observed
    And I validate ORDER_CREATED.causationId equals "fraud_id"
    And I validate ORDER_CREATED occurs only after BOTH payment AND fraud success

    Then I verify hold processing completes implicitly
    Then the "MIGRATION_INITIATED" event should be observed
    Then the "MIGRATION_COMPLETED" event should be observed
    And I verify order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | ItemType      | ItemSKU   | Channel     | MembershipType | PaymentType | CardType   | Quantity | UnitPrice | PricePerPound | EstimatedWeight | AgeRestriction | LineTotal | Subtotal | Tax   | PickupFee | GrandTotal | PickupClub |
      | Regular       | 987654321 | WCS         | Club           | CreditCard  | Visa       | 1        | 29.99     | N/A           | N/A             | N/A            | 29.99     | 29.99    | 2.10  | 0.00      | 32.09      | 1234       |
      | Grocery       | 111222333 | Mobile App  | Club           | DebitCard   | Visa       | 2        | 4.99      | N/A           | N/A             | N/A            | 9.98      | 9.98     | 0.70  | 0.00      | 10.68      | 5678       |
      | Weighted      | 555666777 | WCS         | Club           | CreditCard  | Mastercard | 1        | 8.99      | 8.99          | 2.50            | N/A            | 22.48     | 22.48    | 1.57  | 0.00      | 24.05      | 1234       |
      | AgeRestricted | 888999000 | WCS         | Club           | CreditCard  | Visa       | 1        | 15.99     | N/A           | N/A             | 21             | 15.99     | 15.99    | 1.12  | 0.00      | 17.11      | 1234       |
      | Bulky         | 777888999 | Call Center | Club           | CreditCard  | Visa       | 1        | 299.99    | N/A           | N/A             | N/A            | 299.99    | 299.99   | 21.00 | 0.00      | 320.99     | 1234       |

  # ==================================================================================
  # SC_BOPIC_002: MULTI-LINE ORDER SCENARIOS
  # Coverage: Multiple lines, mixed quantities
  # Each example runs as a separate scenario
  # ==================================================================================

  @SC_BOPIC_002 @multi-line
  Scenario Outline: BOPIC Multi-Line Order - <LineCount> Lines with <QuantityPattern>
    Given a cart payload from WCS with:
      | Field                  | Value              |
      | fulfillmentType        | BOPIC              |
      | channel                | <Channel>          |
      | membershipType         | Club               |
      | lines                  | <LineCount>        |
    And order lines are configured as:
      | LineNo | ItemSKU   | ItemType | Quantity   | UnitPrice | LineTotal |
      | 1      | <SKU1>    | <Type1>  | <Qty1>     | <Price1>  | <Total1>  |
      | 2      | <SKU2>    | <Type2>  | <Qty2>     | <Price2>  | <Total2>  |
      | 3      | <SKU3>    | <Type3>  | <Qty3>     | <Price3>  | <Total3>  |
    And payment method is configured as:
      | Field            | Value              |
      | paymentType      | CreditCard         |
    And pickup club is "1234"
    And cart totals are:
      | Field              | Value              |
      | subtotal           | <Subtotal>         |
      | tax                | <Tax>              |
      | grandTotal         | <GrandTotal>       |

    When the order payload is routed to OES for processing

    Then the "ORDER_INITIATED" event should be observed
    And I validate ORDER_INITIATED payload contains <LineCount> order lines
    And I capture ORDER_INITIATED.eventId as "init_id"

    Then the "PAYMENT_SAVED" event should be observed
    And I validate PAYMENT_SAVED.causationId equals "init_id"
    And I validate PAYMENT_SAVED payload contains:
      | Field               | Value              |
      | totalAuthorized     | <GrandTotal>       |
    And I capture PAYMENT_SAVED.eventId as "payment_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be observed
    And I validate FRAUD_RESPONSE_SUCCESS.causationId equals "payment_id"
    And I capture FRAUD_RESPONSE_SUCCESS.eventId as "fraud_id"

    Then the "ORDER_CREATED" event should be observed
    And I validate ORDER_CREATED.causationId equals "fraud_id"
    And I validate ORDER_CREATED payload contains all <LineCount> lines

    Then I verify migration workflow completes
    And I verify order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | LineCount | QuantityPattern    | Channel    | SKU1      | Type1   | Qty1 | Price1 | Total1 | SKU2      | Type2   | Qty2 | Price2 | Total2 | SKU3      | Type3   | Qty3 | Price3 | Total3 | Subtotal | Tax  | GrandTotal |
      | 3         | Single Qty Each    | WCS        | 111111111 | Regular | 1    | 19.99  | 19.99  | 222222222 | Regular | 1    | 24.99  | 24.99  | 333333333 | Regular | 1    | 14.99  | 14.99  | 59.97    | 4.20 | 64.17      |
      | 2         | Mixed Quantities   | Mobile App | 444444444 | Regular | 5    | 3.99   | 19.95  | 555555555 | Grocery | 2    | 7.49   | 14.98  | N/A       | N/A     | N/A  | N/A    | N/A    | 34.93    | 2.45 | 37.38      |

  # ==================================================================================
  # SC_BOPIC_006: SERVICE LINE SCENARIOS
  # SC_BOPIC_009-011: PAYMENT METHOD SCENARIOS
  # Each example runs as a separate scenario
  # ==================================================================================

  @SC_BOPIC_006 @SC_BOPIC_009 @SC_BOPIC_010 @SC_BOPIC_011 @payment-methods @service-line
  Scenario Outline: BOPIC Order with <PaymentType> Payment and <FeeScenario>
    Given a cart payload from WCS with:
      | Field                  | Value              |
      | fulfillmentType        | BOPIC              |
      | channel                | <Channel>          |
      | membershipType         | <MembershipType>   |
      | lines                  | 1                  |
    And line 1 contains:
      | Field         | Value              |
      | itemSKU       | <ItemSKU>          |
      | itemType      | Regular            |
      | quantity      | <Quantity>         |
      | unitPrice     | <UnitPrice>        |
      | lineTotal     | <LineTotal>        |
    And payment method is configured as:
      | Field            | Value              |
      | paymentType      | <PaymentType>      |
      | cardType         | <CardType>         |
      | giftCardBalance  | <GiftCardBalance>  |
      | paypalEmail      | <PayPalEmail>      |
      | walletBalance    | <WalletBalance>    |
    And service line configuration is:
      | Field         | Value              |
      | pickupFee     | <PickupFee>        |
      | feeReason     | <FeeReason>        |
    And pickup club is "1234"
    And cart totals are:
      | Field              | Value              |
      | subtotal           | <Subtotal>         |
      | tax                | <Tax>              |
      | pickupFee          | <PickupFee>        |
      | grandTotal         | <GrandTotal>       |

    When the order payload is routed to OES for processing

    Then the "ORDER_INITIATED" event should be observed
    And I capture ORDER_INITIATED.eventId as "init_id"

    Then the "PAYMENT_SAVED" event should be observed
    And I validate PAYMENT_SAVED.causationId equals "init_id"
    And I validate PAYMENT_SAVED payload contains:
      | Field               | Value              |
      | paymentType         | <PaymentType>      |
      | totalAuthorized     | <GrandTotal>       |
    And I capture PAYMENT_SAVED.eventId as "payment_id"

    Then the "FRAUD_RESPONSE_SUCCESS" event should be observed
    And I validate FRAUD_RESPONSE_SUCCESS.causationId equals "payment_id"
    And I capture FRAUD_RESPONSE_SUCCESS.eventId as "fraud_id"

    Then the "ORDER_CREATED" event should be observed
    And I validate ORDER_CREATED.causationId equals "fraud_id"

    Then I verify migration workflow completes
    And I verify order reaches terminal state "MIGRATED_TO_STERLING"

    Examples:
      | PaymentType | CardType   | Channel     | MembershipType | ItemSKU   | Quantity | UnitPrice | LineTotal | Subtotal | Tax  | PickupFee | GrandTotal | FeeScenario     | FeeReason      | GiftCardBalance | PayPalEmail          | WalletBalance |
      | CreditCard  | Visa       | WCS         | Club           | 987654321 | 1        | 29.99     | 29.99     | 29.99    | 2.10 | 0.00      | 32.09      | No Fee          | N/A            | N/A             | N/A                  | N/A           |
      | CreditCard  | Visa       | WCS         | NonClub        | 666666666 | 1        | 12.99     | 12.99     | 12.99    | 0.91 | 2.99      | 16.89      | With Pickup Fee | NonClubMember  | N/A             | N/A                  | N/A           |
      | GiftCard    | N/A        | WCS         | Club           | 777777777 | 1        | 25.00     | 25.00     | 25.00    | 1.75 | 0.00      | 26.75      | No Fee          | N/A            | 50.00           | N/A                  | N/A           |
      | PayPal      | N/A        | Mobile App  | Club           | 888888888 | 2        | 18.99     | 37.98     | 37.98    | 2.66 | 0.00      | 40.64      | No Fee          | N/A            | N/A             | member@example.com   | N/A           |
      | Wallet      | N/A        | WCS         | ClubPlus       | 999999999 | 1        | 49.99     | 49.99     | 49.99    | 3.50 | 0.00      | 53.49      | No Fee          | N/A            | N/A             | N/A                  | 100.00        |

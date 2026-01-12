# Order Router Rules

## Input
- Cart in WCS format from Checkout

## Feature Evaluation
Order Router retrieves feature definitions and evaluates:
- Delivery Method
- Throttling rules
- Customer Club eligibility

## Header Stamping
Order Router stamps the following headers:

### X-ROUTE-TO-OES
- Y → Order is routed to OES
- N → Order is routed to Sterling

This flag is used for:
- Item-level filtering
- STH orders
- DSV orders

### X-LIFECYCLE-IN-OES
- Y → Order continues full lifecycle in OES
- N → Order is transferred to Sterling after hold processing

## Routing Decision
- If X-ROUTE-TO-OES = Y → Route order payload to OES
- Else → Route order payload to Sterling

## Outcome
- Order Capture is invoked
- ORDER_INITIATED event is emitted

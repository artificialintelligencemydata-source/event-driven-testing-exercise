# Post-Hold Migration Flow (OES vs Sterling)

## Trigger
- HOLD_PROCESS_COMPLETE event is emitted.

## Decision Condition
- Header: X-LIFECYCLE-IN-OES (Y / N)

## Flow when X-LIFECYCLE-IN-OES = Y
1. Update Order Status to "Migration In Progress"
2. Emit MIGRATION_INITIATED event (optional for Order Status Service)
3. Collect all relevant order data for transfer
4. Process order for scheduling
5. Calculate Available for Release Date as:
   - max(Slot Time - X hours, Remorse Window)
6. For instant orders, Release Time = ASAP
7. Emit ORDER_SCHEDULED event
8. Emit MIGRATION_COMPLETE event
9. All systems listening mark records for purge after configured time

## Flow when X-LIFECYCLE-IN-OES = N
1. Transform order payload to Sterling format
2. Update Order Source to STERLING
3. Transfer order to Sterling for further processing

## Events Involved
- HOLD_PROCESS_COMPLETE
- MIGRATION_INITIATED
- ORDER_SCHEDULED
- MIGRATION_COMPLETE

## Notes
- Migration initiated event is optional for Order Status Service
- All processing is event-driven; no synchronous orchestration

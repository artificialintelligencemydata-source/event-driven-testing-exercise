Flow: Initial Order Routing

1. Checkout produces cart in WCS format
2. Order Router fetches feature flags
3. Router evaluates:
   - Delivery Method
   - Throttling
   - Customer Club
4. Router stamps headers:
   - X-ROUTE-TO-OES (Y/N)
   - X-LIFECYCLE-IN-OES (Y/N)
5. Decision:
   - If X-ROUTE-TO-OES=Y → Route to OES
   - 	-If X-LIFECYCLE_IN_OES=Y -> Continue with OES 
		-ELSE Rout to sterling post order Hold Release.
   - Else → Route to Sterling
6. Order Capture emits ORDER_INITIATED

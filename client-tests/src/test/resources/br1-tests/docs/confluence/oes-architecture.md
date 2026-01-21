# OES Architecture & Testing Principles

## Architectural Style
- Event-driven choreography-based system
- Observer State Pattern
- No central orchestrator

## Key Characteristics
- Services react to events independently
- Each domain owns its own state transitions
- Observer provides end-to-end visibility
- Events are the system contract

## Domains
- Order Domain
- Hold Domain
- Payment Domain
- Allocation Domain
- Notes Domain
- Event Store
- Event Dispatcher

## Testing Implications
- Tests must validate:
  - State transitions
  - Event emission
- Tests must NOT:
  - Validate internal orchestration
  - Rely on synchronous processing
  - Poll databases or queues

## Supported Patterns
- Idempotent event handling
- Transactional outbox
- Event replay and purge
- Graceful degradation

## AUTWIT Alignment
- Tests express business intent only
- AUTWIT handles waiting, resumption, and orchestration

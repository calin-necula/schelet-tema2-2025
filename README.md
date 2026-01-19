Bug tracker pro
--

## **Structure:**
- `cod/command/` command contracts, a factory, an invoker, and concrete actions for creating tickets, assigning owners, shifting status, adding comments, searching, viewing history, and generating reports including undo variants.
- `cod/factory/``TicketFactory` centralizes how `Bug` and `FeatureRequest` instances are built, hiding construction details from callers.
- `cod/model/` domain objects `Ticket`, `Bug`, `FeatureRequest`, `Comment`, `Milestone`, `TicketAction`, `User` plus `Developer`, `Reporter`, `Manager` and enums `Priority`, `Status`, `Role`, `Seniority`, `TicketType` that define the lifecycle and semantics of work items.
- `cod/strategy/` interchangeable calculators for metrics and customer impact, letting reports plug in the metric logic they need.
- `cod/database/` `Database` holds tickets, users, milestones, and notifications in memory, acting as a shared stateful store.
- `cod/utils/` date helpers and a `NotificationManager` that fans out updates on key ticket events.
- `main/App` transfers input to commands, acting as the thin entry point that delegates all real work to the command layer.

## **Domain & Lifecycle:**
- Tickets carry `Priority`, `Status`, `TicketType`, assignees, and threaded `Comment` history; `TicketAction` snapshots every mutation for an auditable trail.
- Users are classified as `Reporter`, `Developer`, `Manager` with `Role` and `Seniority`, influencing responsibilities and how actions are recorded.
- Milestones group related work; creation and listing run through dedicated commands.

## **Commands & Workflows:**
- Creation: spin up `Bug` or `FeatureRequest` via the factory and a create command.
- Assignment: bind tickets to developers, optionally reassigning as work shifts.
- Status flow: advance or roll back through lifecycle stages, with history entries and notifications emitted on change.
- Collaboration: append comments to keep discussion attached to the ticket timeline.
- Viewing: list tickets, filter assigned items, inspect history, milestones, and notifications.
- Reporting: generate app stability, performance, resolution efficiency, ticket risk, and customer‑impact reports by composing the appropriate strategies.

## **Notifications & History:**
- `NotificationManager` alerts interested parties when tickets are created, reassigned, commented, or moved in status.
- `TicketAction` memorizes every notable event with timestamps by using `DateUtils`, ensuring you can reconstruct what happened and when.

## **Data Handling:**
- The in‑memory `Database` acts as the authoritative store for tickets, users, milestones, and pending notifications, giving the commands a shared, consistent view of system state.
- **Command Pattern:** Each user action assigning, commenting, status transitions, starting testing, reporting, viewing history,notifications is encapsulated as a command, executed through an invoker; undo commands mirror the forward actions where relevant.
- **Factory Pattern:** `TicketFactory` unifies ticket creation, letting clients request a type without binding to concrete constructors.
- **Strategy Pattern:** Metrics and customer‑impact scoring for reports are pluggable via `MetricStrategy` and `CustomerImpactStrategy`, keeping reporting logic decoupled and swappable.
- **Singleton‑Style Store:** `Database` is the single in‑memory hub so all commands see consistent tickets, users, milestones, and notification queues.



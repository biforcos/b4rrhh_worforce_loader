# ADR-002 — Lifecycle Simulation V1 in b4rrhh-workforce-loader

## Status
Proposed

## Context

`b4rrhh-workforce-loader` started as a simple external CLI-style tool for massive employee hire execution against B4RRHH public APIs.

That first scope was useful to:
- validate the hire workflow;
- test volume with real public APIs;
- generate synthetic employees with reproducible data;
- stress basic catalog resolution and request execution.

However, the real long-term goal of the tool is broader.

The tool must evolve into a lifecycle simulation utility capable of generating realistic employee history over time, including:
- hire
- terminate
- rehire
- future vertical changes such as contract, labor classification, work center, cost center, etc.

At this stage, the next natural milestone is to introduce lifecycle simulation focused only on:
- hire
- terminate
- rehire

This aligns with the public lifecycle workflows already exposed by B4RRHH and avoids prematurely mixing vertical mutations before the lifecycle backbone is stable.

## Decision

`b4rrhh-workforce-loader` will evolve from a mass-hire tool into a lifecycle simulation tool.

### V1 lifecycle scope

This iteration includes only these lifecycle actions:
- HIRE
- TERMINATE
- REHIRE

This iteration explicitly excludes:
- work center changes
- cost center changes
- contract changes
- labor classification changes
- contacts, addresses, identifiers
- concurrency and batching strategies beyond the current simple execution model

## Design Principles

### 1. Scenario-based simulation

The loader must stop thinking only in terms of “employees to create”.

Instead, it must simulate employee lifecycle scenarios.

Each employee may follow a path such as:
- hire only
- hire + terminate
- hire + terminate + rehire

### 2. Events, not random API calls

Simulation must be modeled as ordered lifecycle events, not as isolated endpoint calls.

The minimum event types for V1 are:
- HIRE
- TERMINATE
- REHIRE

### 3. Chronological coherence

Lifecycle events must always be temporally coherent.

Rules:
- terminateDate must be strictly later than hireDate
- rehireDate must be strictly later than terminationDate
- no impossible timelines are generated

### 4. Public API only

The loader continues to consume only canonical public APIs.
It must not access the database directly.

### 5. Reproducibility

Simulation must remain reproducible through configurable random seed.

## Proposed Model

### Scenario

Introduce an `EmployeeLifecycleScenario` model that represents the timeline of one employee.

It contains:
- synthetic employee base data
- ordered lifecycle events

### Lifecycle Event

Introduce a simple lifecycle event model with:
- eventType
- effectiveDate

Optional payload fields may be added depending on event type.

### Event Types in V1

- `HIRE`
- `TERMINATE`
- `REHIRE`

### Scenario Generation

The generator must produce realistic but simple scenarios based on configurable rates.

Example configurable parameters:
- total employee count
- terminate rate
- rehire rate over terminated employees

### Scenario Execution

Scenarios must be executed in chronological order.

Execution rules:
- if HIRE fails, abort the rest of that employee scenario
- if a later event fails, record the error and continue with the next employee

## Configuration

Configuration should evolve to include lifecycle simulation parameters such as:
- employee count
- seed
- hire date range
- terminate rate
- rehire rate
- optional termination / rehire date windows

## Reporting

The report must be extended to show counts by lifecycle action:
- hires requested / success / failed
- terminations requested / success / failed
- rehires requested / success / failed

## Consequences

### Positive
- produces realistic employee historical states
- stresses the real lifecycle model instead of only creation
- prepares the loader for future vertical mutations
- keeps the scope manageable

### Negative
- adds a scenario/event layer to the loader
- introduces more date-planning logic
- increases execution complexity compared to mass-hire-only mode

## Not Yet Included

This ADR does not yet introduce:
- vertical mutation events
- cached reference pools per employee event
- concurrency
- advanced retry strategies
- generic event engines

## Summary

The loader evolves into a lifecycle simulator.

The first lifecycle simulation iteration is intentionally narrow:
- hire
- terminate
- rehire

This creates the historical backbone needed before adding vertical changes.
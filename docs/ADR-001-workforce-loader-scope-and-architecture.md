# ADR-001 — Scope and Architecture of b4rrhh-workforce-loader

## Status
Proposed

## Context

B4RRHH already exposes business-oriented employee lifecycle operations and public APIs based on functional business keys.

The main project follows these core rules:
- public APIs use business keys, never technical IDs;
- employee identity is:
  - ruleSystemCode
  - employeeTypeCode
  - employeeNumber
- lifecycle actions such as hire, terminate and rehire are modeled as workflows, not as raw CRUD orchestration by the caller.

The project now needs a separate tool to create employees massively in order to:
- stress-test the platform with realistic volume;
- validate lifecycle workflows under larger data sets;
- detect performance, consistency and functional validation issues;
- generate repeatable synthetic employee populations.

This tool is not part of the core domain model of B4RRHH and should not pollute the main repositories.

## Decision

A separate repository/tool named `b4rrhh-workforce-loader` will be created as an external application that consumes B4RRHH public APIs.

### Main rules

1. The loader is a separate tool, not a new bounded context inside the main B4RRHH product.
2. The loader must consume only public/canonical APIs and workflows.
3. The loader must never write directly to the database.
4. The loader must never use technical IDs as public identity.
5. The loader must generate payloads using the canonical employee business key:
   - ruleSystemCode
   - employeeTypeCode
   - employeeNumber
6. The first supported operation is massive `hire`.
7. `terminate` and `rehire` may be added later.
8. The tool must support deterministic/repeatable runs through a configurable random seed.
9. The tool must produce execution reports with success/failure breakdown.

## Architectural Approach

The loader will be implemented as a small CLI-style Spring Boot application.

Recommended internal structure:

- application
- domain
- infrastructure

This repo does not need to replicate the vertical-first architecture of the main B4RRHH domain because it is not a business domain module of the core product. It is an external orchestration/testing utility.

## Initial Scope (V1)

V1 includes:
- YAML-based configuration;
- synthetic employee generation;
- massive execution of Hire Employee workflow;
- dry-run mode;
- simple execution report.

V1 excludes:
- direct DB access;
- generic import engine;
- UI;
- terminate/rehire support;
- automatic runtime discovery of all catalogs.

## Consequences

### Positive
- preserves clean boundaries with the main project;
- tests the platform through its canonical public interface;
- supports realistic mass-volume validation;
- avoids coupling to persistence internals;
- encourages repeatable, controlled test scenarios.

### Negative
- some catalog/reference values must be configured explicitly at first;
- the tool adds one more repo to maintain;
- V1 is intentionally narrow and not a universal import framework.

## Summary

`b4rrhh-workforce-loader` is a separate external tool designed to stress and validate B4RRHH through its canonical lifecycle APIs, starting with massive `hire` operations and respecting business-key-only public identity.
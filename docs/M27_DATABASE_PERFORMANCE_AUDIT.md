# M27 — Database Performance Audit

## Status

**Complete**

This milestone measured high-risk database access paths, removed two
participant-dependent N+1 query patterns, reviewed repository access
patterns, aligned production indexes with high-use ordered queries, and
verified all changes through automated tests and PostgreSQL containers.

## Scope

The audit covered:

- group activity timeline loading
- shared-expense participant validation and user loading
- repository filtering and ordering patterns
- Flyway index definitions
- PostgreSQL migration execution
- H2 integration-test compatibility
- full regression and JaCoCo generation

## Query Measurement Method

Hibernate statistics were enabled only for the test profile:

```properties
spring.jpa.properties.hibernate.generate_statistics=true
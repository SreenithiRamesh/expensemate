# M27 — Database Performance Audit

## Status

**Complete**

M27 measured high-risk database access paths, removed two participant-dependent
N+1 query patterns, reviewed repository access patterns, aligned production
indexes with high-use ordered queries, and verified the changes through
automated tests and a PostgreSQL container.

## Scope

The audit covered:

- group activity timeline loading
- shared-expense participant validation
- shared-expense user loading
- repository filtering and ordering patterns
- Flyway index definitions
- PostgreSQL migration execution
- H2 integration-test compatibility
- full regression testing and JaCoCo generation

## Measurement method

Hibernate statistics are enabled only for the test profile:

```properties
spring.jpa.properties.hibernate.generate_statistics=true
```

`HibernateQueryCountSupport` resets Hibernate statistics immediately before
the measured service operation and reads the prepared-statement count after
the operation completes.

Tests clear the persistence context before measuring so that Hibernate’s
first-level cache does not conceal lazy-loading queries.

## Group activity timeline

### Problem

Loading ten activities with ten distinct actors caused one additional user
query per activity because each actor was loaded lazily.

### Optimization

`GroupActivityRepository` now fetches the actor required for response mapping
as part of the activity query.

### Result

| Measurement | Before | After | Reduction |
|---|---:|---:|---:|
| Prepared statements | 14 | 4 | 71.4% |

The optimized query count remains bounded as the number of activities grows.

## Shared-expense creation

### Problem

Participant validation and participant-user loading performed repeated
repository calls for individual users. The statement count therefore increased
with the number of participants.

### Optimization

The write flow now:

- loads participant users in one bulk query
- checks group membership in bulk
- validates missing or unauthorized participants in memory
- preserves the existing financial and authorization rules

### Result

The test uses ten participants.

| Measurement | Before | After | Reduction |
|---|---:|---:|---:|
| Prepared statements | 35 | 16 | 54.3% |

The participant-dependent user and membership N+1 patterns were removed.

## Index audit

Flyway migration
`V13__optimize_high_use_query_indexes.sql` adds indexes aligned with frequent
group-filtered and date-ordered queries.

Verified PostgreSQL indexes include:

```text
idx_shared_expenses_group_date_created
    (group_id, expense_date DESC, created_at DESC)

idx_settlements_group_settled_at
    (group_id, settled_at DESC)
```

The migration uses explicit index definitions and does not modify financial
data.

## Verification

The following checks passed:

- activity performance integration test
- shared-expense performance integration test
- settlement idempotency concurrency test
- shared-expense idempotency concurrency test
- complete Maven verification suite
- JaCoCo report generation
- Docker image build
- PostgreSQL startup and health check
- Flyway migrations V1 through V13
- production index inspection through `pg_indexes`
- Actuator health check

## Evidence

```text
M27_ACTIVITY_OPTIMIZED_QUERY_COUNT=4
M27_SHARED_EXPENSE_OPTIMIZED_PARTICIPANTS=10
M27_SHARED_EXPENSE_OPTIMIZED_STATEMENT_COUNT=16
Tests run: 272, Failures: 0, Errors: 0, Skipped: 0
Flyway V13: success
Actuator health: UP
```

Expected unique-key warnings from the concurrency tests are intentional. They
prove that database constraints reject competing duplicate financial writes.

## Conclusion

M27 is complete. The measured N+1 problems were removed, high-use indexes were
added and verified on PostgreSQL, query-count regression tests were introduced,
and the complete backend regression suite remains green.
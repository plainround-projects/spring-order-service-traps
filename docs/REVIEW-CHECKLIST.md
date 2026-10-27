# Review checklist for a pull request that touches JPA or transactions

Written from the three traps in this project (lesson step 6). Each line is a question a reviewer can answer from
the diff, and names the test in this repository that would catch the bug it asks about.
`ReviewChecklistTest` fails the build if a named test does not exist.

1. Does any `this.method()` call rely on a `@Transactional` that the calling method does not itself carry? Caught by `SelfInvocationIT#selfInvocationLeavesAPartialOrderOnFailure`, and by the ArchUnit rule `noSelfInvocationOfTransactionalMethods`.
2. Does a collection access inside a loop (or inside a mapper) risk one query per iteration? Caught by `NPlusOneIT#fetchJoinKeepsQueryCountFlatAt200Orders`.
3. Is a method that writes an audit or log record called from inside a larger transaction that it must survive independently of? Caught by `AuditIT#auditWithRequiresNewSurvivesRollback`.
4. Is `spring.jpa.open-in-view` explicitly `false`, so a lazy load outside a transaction fails in development instead of working by accident? Caught by `OpenInViewIT#aLazyLoadOutsideATransactionFailsWhenOpenInViewIsOff`.
5. Does a `REQUIRES_NEW` method live on a different bean than its caller, and is there a comment saying why? Caught by `AuditIT#requiresNewOnTheSameBeanIsIgnored` (and the comment on `JpaAuditLog`).

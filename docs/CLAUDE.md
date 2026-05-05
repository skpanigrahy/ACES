# ACES — Autonomous Cognitive Engineering System
## Claude Code Project Instructions

> Read this file completely before generating any code.
> Every class, every method, every config must follow these rules exactly.

---

## 1. What You Are Building

ACES is a **spec-driven AI execution platform** built with Java 21 + Spring Boot 3.3 + Spring AI + Oracle 23ai on JPMorgan Chase private cloud.

It has two runtime layers:
- **ARIA** = Governance layer (trust, risk, decisions, audit) — separate service
- **ACES** = Execution layer (agents, tools, workflows) — THIS codebase

Every AI agent execution must pass through ARIA before ACES executes it.

---

## 2. Package Structure — NEVER Deviate

```
com.jpmc.aces.api.*           → REST controllers, DTOs, security config
com.jpmc.aces.core.*          → Agentic loop: planner, orchestrator, retriever, synthesizer, critic, memory
com.jpmc.aces.mcp.*           → MCP pipeline: auth, policy, validation, ratelimit, circuitbreaker, tool
com.jpmc.aces.runtime.*       → Agent runtime: registry, router, injector, adapters, reporter
com.jpmc.aces.governance.*    → ARIA client: REST client, Oracle AQ reporter
com.jpmc.aces.observability.* → OTel tracing, Micrometer metrics
com.jpmc.aces.common.*        → Shared records/DTOs/contracts — no business logic, no Spring beans
```

**Rule:** If you cannot determine the module from the package name, the class is in the wrong package.

---

## 3. Technology Stack — Use ONLY These

| Concern | Technology | Notes |
|---------|-----------|-------|
| Runtime | Java 21 | Use records, sealed classes, virtual threads where appropriate |
| Framework | Spring Boot 3.3.x | Latest stable |
| AI | Spring AI 1.x | NOT LangChain4j — Spring AI only |
| Database | Oracle 23ai (private cloud) | Direct JDBC — NO wallet file, NO ADB |
| DB Access | Spring Data JPA + HikariCP | JdbcTemplate for complex Oracle-specific queries |
| DB Migration | Flyway | Every DDL in src/main/resources/db/migration/ |
| Security | Spring Security OAuth2 Resource Server | JWT validation |
| Resilience | Resilience4j 2.x | Circuit breaker, retry, rate limiter |
| Cache | Spring @Cacheable (ConcurrentHashMap) | Phase 1 only — no Redis yet |
| Async Messaging | Oracle Advanced Queuing (AQ) | For ARIA Reporter |
| Observability | Micrometer + OpenTelemetry | Via Spring Boot Actuator |
| Secrets | HashiCorp Vault via Spring Cloud Vault | Never hardcode credentials |
| Testing | JUnit 5 + Mockito + Spring Boot Test | All three required |

**NEVER add:** Redis, Qdrant, Elasticsearch, Kafka, Docker Compose, Kubernetes configs, LangChain4j, wallet files.

---

## 4. Oracle 23ai Connection — Exact Pattern

```yaml
# application.yml — always use this exact structure
spring:
  datasource:
    url: jdbc:oracle:thin:@//${ORACLE_HOST}:${ORACLE_PORT:1521}/${ORACLE_SERVICE}
    username: ${ORACLE_USERNAME}
    password: ${ORACLE_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      connection-test-query: SELECT 1 FROM DUAL
  jpa:
    database-platform: org.hibernate.dialect.OracleDialect
    hibernate:
      ddl-auto: validate
    show-sql: false
```

**NEVER** reference wallet files, `tnsnames.ora`, `cwallet.sso`, or ADB-specific config.

---

## 5. Tenant Isolation — Oracle VPD Pattern

Every service that reads from Oracle must follow this pattern:

```java
// In TenantGuardInterceptor — already implemented in aces-mcp
// Your service does NOT need to add WHERE tenant_id = ? manually
// Oracle VPD handles it at the engine level

// What your service MUST do: never bypass TenantContextHolder
// What Oracle VPD does automatically: filter all rows by tenant_id
```

If you are writing a JPA repository, do NOT add `findByIdAndTenantId()`.
Oracle VPD makes `findById()` automatically tenant-scoped.

---

## 6. Code Style Rules — Enforce All

```java
// ✅ CORRECT: Record for immutable data
public record AgentRequest(String executionId, String taskType, AriaContext ariaContext) {}

// ✅ CORRECT: Constructor injection always
@Service
public class RateLimiterService {
    private final ToolRegistry toolRegistry;
    private final MeterRegistry meterRegistry;

    public RateLimiterService(ToolRegistry toolRegistry, MeterRegistry meterRegistry) {
        this.toolRegistry = toolRegistry;
        this.meterRegistry = meterRegistry;
    }
}

// ❌ WRONG: Field injection
@Autowired
private ToolRegistry toolRegistry;

// ✅ CORRECT: Logging
private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

// ✅ CORRECT: OpenTelemetry span on every public service method
@WithSpan
public AgentResponse execute(AgentRequest request) { ... }

// ✅ CORRECT: Oracle-aware exception handling
catch (DataAccessException e) {
    log.error("Oracle error in {}: {}", getClass().getSimpleName(), e.getMessage());
    throw new AcesDataException("Database operation failed", e);
}
```

---

## 7. The AgentPlugin Contract — Never Change

```java
// com.jpmc.aces.common.plugin.AgentPlugin
public interface AgentPlugin {
    PluginManifest getManifest();
    HealthStatus ping();
    AgentResponse execute(AgentRequest request);
    ExecutionMetadata getExecutionMetadata(String executionId);
    default void cancel(String executionId) {}
    default CostEstimate estimateCost(AgentRequest request) { return null; }
}
```

**Critical:** `agentReasoning` in `AgentResponse` is MANDATORY. If your adapter cannot populate it, throw an `AcesValidationException`. Never return null or empty string.

---

## 8. ARIA Integration — How Every Module Must Call It

```java
// BEFORE execution — always check with ARIA
AriaDecision decision = ariaClient.decide(AriaRequest.builder()
    .agentId(request.agentId())
    .taskType(request.taskType())
    .environment(request.constraints().environment())
    .build());

if (decision.isBlock()) {
    auditLogger.logBlock(request, decision);
    throw new AcesBlockedException(decision.reason(), decision.archaeology());
}

// AFTER execution — always report async via Oracle AQ
ariaReporter.reportAsync(ExecutionRecord.builder()
    .executionId(request.executionId())
    .agentId(request.agentId())
    .tenantId(TenantContextHolder.getTenantId())
    .agentReasoning(response.agentReasoning()) // NEVER null
    .tokenCount(response.tokenCount())
    .status(response.status())
    .build());
```

---

## 9. Flyway Migration Naming

```
V1__create_aces_plugins.sql
V2__create_aces_executions.sql
V3__create_aces_tool_registry.sql
V4__create_aces_tool_invocations.sql
V5__create_aces_audit_log.sql
V6__create_aces_approvals.sql
V7__create_aces_memory_ltm.sql
V8__create_aces_policy_rules.sql
```

Every migration must include:
- `tenant_id VARCHAR2(100)` on every table
- `created_at TIMESTAMP DEFAULT SYSTIMESTAMP`
- A comment explaining the purpose
- VPD policy setup at the bottom of the file (as a comment — DBA runs separately)

---

## 10. Test Requirements — All Three Required

Every class must have a corresponding test class:

```java
// Unit test — pure logic, no Spring context
@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest { ... }

// Integration test — with Spring context, Oracle TestContainers or mock datasource
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class RateLimiterServiceIT { ... }

// At minimum: happy path, failure path, tenant isolation verification
```

---

## 11. Non-Negotiables — Fail the Build If Violated

The CI pipeline checks for these. Do not commit code that violates them:

1. No `@Autowired` field injection anywhere
2. No hardcoded credentials, URLs, or hostnames
3. No `System.out.println()` — use SLF4J
4. No empty `agentReasoning` in `AgentResponse`
5. No direct Oracle VPD bypass (no `WHERE tenant_id = ?` in app code)
6. No `hibernate.ddl-auto: create` or `update` — always `validate`
7. No missing `@WithSpan` on public service methods
8. No Flyway migrations that lack `tenant_id` column

---

## 12. Module Completion Checklist

Before raising a PR, every module must pass:

- [ ] `mvn clean install` passes with zero test failures
- [ ] All public service methods have `@WithSpan` annotation
- [ ] All Oracle interactions use Spring Data JPA or JdbcTemplate (no raw JDBC)
- [ ] Flyway migration exists for any new tables
- [ ] Unit tests cover: happy path, failure path, edge case
- [ ] Integration test confirms Oracle connectivity
- [ ] No compiler warnings
- [ ] `agentReasoning` populated and validated in any AgentPlugin implementation
- [ ] ARIA check called before any agent execution
- [ ] ARIA report sent after any agent execution

---

*This file is the single source of truth for all code generation in this project.*
*If Claude generates something that contradicts this file, this file wins.*

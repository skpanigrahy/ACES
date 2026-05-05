# ACES Module Assignments — One Per Engineer
## Claude Code Generation Specs

---

## How This Works

Each engineer gets one module spec file.
They open Claude Code in their module directory.
CLAUDE.md is at the project root — Claude reads it automatically.
They say to Claude: "Generate this module from the spec file."
Claude generates all classes, tests, and configs.
Engineer reviews every line. Fixes issues. Raises PR.

---

## Engineer 1: aces-mcp (MCP Control Pipeline)
**Spec file:** `specs/aces-mcp-ratelimiter-spec.md` (and companion specs below)
**Estimated generation time:** 2 hours with Claude Code
**Review time:** 4–6 hours

### Files Claude Will Generate
```
aces-mcp/src/main/java/com/jpmc/aces/mcp/
├── auth/
│   ├── JwtAuthFilter.java
│   └── JwtAuthFilterTest.java
├── tenant/
│   ├── TenantGuardInterceptor.java
│   ├── TenantContextHolder.java
│   └── TenantGuardInterceptorTest.java
├── policy/
│   ├── PolicyEvaluationService.java
│   ├── PolicyRule.java (record)
│   ├── PolicyVerdict.java (record)
│   └── PolicyEvaluationServiceTest.java
├── ratelimit/
│   ├── RateLimiterService.java
│   ├── RateLimiterConfig.java
│   ├── ToolRateLimit.java (record)
│   └── RateLimiterServiceTest.java
├── circuitbreaker/
│   ├── CircuitBreakerConfig.java
│   └── CircuitBreakerConfigTest.java
├── tool/
│   ├── ToolExecutionEngine.java
│   ├── ToolRegistry.java
│   ├── ToolInvocation.java (record)
│   ├── AuditLogger.java
│   └── ToolExecutionEngineTest.java
└── exception/
    ├── RateLimitExceededException.java
    ├── PolicyDeniedException.java
    └── ToolExecutionException.java
```

### Prompt to give Claude Code
```
Read CLAUDE.md first. Then generate the complete aces-mcp module.

This module is the MCP control pipeline for ACES. It handles:
- JWT authentication (OAuth 2.1 + JWKS)
- Tenant isolation via Oracle VPD context setting
- YAML policy evaluation (deny-by-default rules from Oracle)
- Rate limiting (token bucket per tenant per tool, Spring Cache)
- Circuit breaking (Resilience4j per downstream tool)
- Tool execution orchestration (auth → policy → rate → circuit → execute → audit)
- Audit logging to Oracle ACES_AUDIT_LOG (append-only, no UPDATE/DELETE)

Use Spring Security for JWT. Use Resilience4j for circuit breaker.
Use Spring @Cacheable (ConcurrentHashMap) for rate limit buckets.
Use Oracle JDBC (via Spring Data) for policy rules and audit log.
Every Oracle table access must rely on VPD — do NOT add WHERE tenant_id manually.
Every public method gets @WithSpan for OpenTelemetry.
Generate all unit tests with Mockito.
```

---

## Engineer 2: aces-core (Agentic Orchestration)
**Estimated generation time:** 3 hours with Claude Code

### Files Claude Will Generate
```
aces-core/src/main/java/com/jpmc/aces/core/
├── planner/
│   ├── PlannerService.java
│   ├── RetrievalPlan.java (record)
│   └── PlannerServiceTest.java
├── orchestrator/
│   ├── OrchestratorService.java
│   ├── OrchestrationResult.java (record)
│   └── OrchestratorServiceTest.java
├── retriever/
│   ├── RetrieverService.java
│   ├── ToolFindings.java (record)
│   └── RetrieverServiceTest.java
├── synthesizer/
│   ├── SynthesizerService.java
│   ├── SynthesisResult.java (record)
│   └── SynthesizerServiceTest.java
├── critic/
│   ├── CriticService.java
│   ├── CriticVerdict.java (enum: APPROVE, REVISE)
│   └── CriticServiceTest.java
├── memory/
│   ├── MemoryService.java
│   ├── MemoryEntry.java (record)
│   └── MemoryServiceTest.java
└── config/
    └── AcesCoreConfig.java
```

### Prompt to give Claude Code
```
Read CLAUDE.md first. Then generate the complete aces-core module.

This module is the agentic orchestration loop for ACES:
Planner → Orchestrator → Retriever → Synthesizer → Critic

Rules:
- OrchestratorService drives ONE revise loop. Max 1 revision cycle. Hard stop.
- RetrieverService calls tools via aces-mcp. Max 6 tool iterations (ATBA counter).
- SynthesizerService uses Spring AI ChatClient to draft response with citations.
- CriticService uses Spring AI ChatClient to evaluate. Returns CriticVerdict enum.
- MemoryService: STM uses Spring @Cacheable. LTM uses Oracle AI Vector Search.
  For Oracle Vector Search: use JDBC with SQL: SELECT content FROM aces_memory_ltm
  WHERE tenant_id = :tenantId ORDER BY VECTOR_DISTANCE(embedding, :queryVector, COSINE)
  FETCH FIRST 5 ROWS ONLY
- AcesCoreConfig provides Spring AI ChatClient beans with model routing.
- Every public method gets @WithSpan.
- AgentContextHolder is thread-local — use it in every service for tenantId and traceId.
Generate all unit tests using Mockito. Mock Spring AI ChatClient in tests.
```

---

## Engineer 3: aces-runtime (Agent Runtime)
**Estimated generation time:** 2 hours with Claude Code

### Files Claude Will Generate
```
aces-runtime/src/main/java/com/jpmc/aces/runtime/
├── registry/
│   ├── AgentPluginRegistry.java
│   ├── PluginHealthMonitor.java
│   └── AgentPluginRegistryTest.java
├── router/
│   ├── AgentExecutionRouter.java
│   └── AgentExecutionRouterTest.java
├── injector/
│   ├── ARIAContextInjector.java
│   └── ARIAContextInjectorTest.java
├── execution/
│   ├── AgentExecutionService.java
│   └── AgentExecutionServiceTest.java
├── reporter/
│   ├── ARIAReporter.java
│   ├── ExecutionRecordPublisher.java
│   └── ARIAReporterTest.java
└── adapter/
    ├── ClaudeAgentAdapter.java
    ├── CopilotAgentAdapter.java
    └── ClaudeAgentAdapterTest.java
```

### Prompt to give Claude Code
```
Read CLAUDE.md first. Then generate the complete aces-runtime module.

This module manages the agent execution lifecycle:

AgentPluginRegistry: loads all AgentPlugin implementations from Oracle ACES_PLUGINS table.
  Refreshes every 60 seconds via @Scheduled. Thread-safe ConcurrentHashMap internally.

AgentExecutionRouter: maps agentId to correct AgentPlugin. Validates plugin is ACTIVE.
  Throws AcesPluginNotFoundException if agentId not found.

ARIAContextInjector: calls ARIA REST API before every execution.
  Gets AriaDecision. If BLOCK throws AcesBlockedException with archaeology URL.
  If ALLOW or WARN: builds AriaContext and returns it for injection into AgentRequest.

AgentExecutionService: orchestrates full lifecycle:
  1. Validate executionId not already processed (Oracle idempotency check)
  2. Call ARIAContextInjector to get AriaContext
  3. Route to agent via AgentExecutionRouter
  4. Call agent.execute(request with ariaContext injected)
  5. Validate response.agentReasoning() is not null/blank — throw if it is
  6. Call ARIAReporter.reportAsync() — non-blocking

ARIAReporter: publishes ExecutionRecord to Oracle AQ.
  Use Spring's JmsTemplate configured for Oracle AQ.
  Must be async — use @Async with dedicated thread pool.
  Retry 3 times with exponential backoff on failure.

ClaudeAgentAdapter: implements AgentPlugin using Spring AI's AnthropicChatClient.
  agentReasoning must be extracted from the model response.
  If model does not include reasoning, generate it from the task description.

CopilotAgentAdapter: stub implementation for now — logs the request and returns
  a mock AgentResponse with populated agentReasoning for demo purposes.

Every public method gets @WithSpan. Constructor injection throughout.
```

---

## Engineer 4: aces-api + aces-governance (API Layer + ARIA Client)
**Estimated generation time:** 2 hours with Claude Code

### Files Claude Will Generate
```
aces-api/src/main/java/com/jpmc/aces/api/
├── controller/
│   ├── ExecutionController.java
│   ├── ToolController.java
│   ├── PluginController.java
│   └── ApprovalController.java
├── dto/
│   ├── ExecuteRequest.java (record)
│   ├── ExecuteResponse.java (record)
│   ├── ToolInvokeRequest.java (record)
│   └── ApprovalRequest.java (record)
├── security/
│   └── SecurityConfig.java
└── exception/
    └── GlobalExceptionHandler.java

aces-governance/src/main/java/com/jpmc/aces/governance/
├── client/
│   ├── ARIAClient.java
│   ├── AriaRequest.java (record)
│   ├── AriaDecision.java (record)
│   └── ARIAClientTest.java
└── reporter/
    ├── ARIAReporter.java
    └── OracleAQConfig.java
```

### Prompt to give Claude Code
```
Read CLAUDE.md first. Generate aces-api and aces-governance modules.

aces-api:
- ExecutionController: POST /api/v1/execute, GET /api/v1/execute/{id},
  GET /api/v1/execute/{id}/archaeology
- ToolController: POST /api/v1/tools/invoke, GET /api/v1/tools, POST /api/v1/tools
- PluginController: GET /api/v1/plugins, POST /api/v1/plugins, GET /api/v1/plugins/{id}/health
- ApprovalController: POST /api/v1/approvals/{id}/approve
- SecurityConfig: OAuth2 resource server, JWT, stateless, permit /actuator/health
- GlobalExceptionHandler: handles AcesBlockedException (403), AcesValidationException (422),
  RateLimitExceededException (429), PluginNotFoundException (404)
- All DTOs as Java records with Bean Validation annotations

aces-governance:
- ARIAClient: Spring RestClient calling ARIA REST API.
  Base URL from ${aces.aria.base-url} (Vault-injected).
  POST /api/v1/aria/decide → returns AriaDecision
  CircuitBreaker around ARIA calls (if ARIA down → default to REQUIRES_APPROVAL, never BLOCK)
  Add @Retry(3) and @CircuitBreaker("aria") via Resilience4j

- OracleAQConfig: configures JmsTemplate for Oracle AQ.
  Queue name: ACES_EXECUTION_REPORTS_QUEUE
  ConnectionFactory using Oracle AQ JDBC connection

Generate all controllers with proper @Valid, @RequestBody, @PathVariable.
Every controller method gets @WithSpan.
```

---

## Engineer 5: Data Engineer — Oracle Schema + aces-common
**Estimated generation time:** 1.5 hours with Claude Code

### Files Claude Will Generate
```
aces-common/src/main/java/com/jpmc/aces/common/
├── plugin/
│   ├── AgentPlugin.java (interface)
│   ├── PluginManifest.java (record)
│   ├── HealthStatus.java (record)
│   └── CostModel.java (record)
├── model/
│   ├── AgentRequest.java (record)
│   ├── AgentResponse.java (record)
│   ├── AriaContext.java (record)
│   ├── ExecutionRecord.java (record)
│   ├── AriaDecision.java (record)
│   ├── ExecutionStatus.java (enum)
│   └── PluginLayer.java (enum)
└── exception/
    ├── AcesException.java (base)
    ├── AcesBlockedException.java
    ├── AcesValidationException.java
    └── AcesDataException.java

db/migration/
├── V1__create_aces_plugins.sql
├── V2__create_aces_executions.sql
├── V3__create_aces_tool_registry.sql
├── V4__create_aces_tool_invocations.sql
├── V5__create_aces_audit_log.sql
├── V6__create_aces_approvals.sql
├── V7__create_aces_memory_ltm.sql
└── V8__create_aces_policy_rules.sql
```

### Prompt to give Claude Code
```
Read CLAUDE.md first. Generate aces-common module and all Flyway migrations.

aces-common rules:
- AgentPlugin interface: exactly as specified in CLAUDE.md section 7
- All models as Java records (immutable)
- AgentResponse must have validation in compact constructor:
  if agentReasoning == null or blank throw AcesValidationException
- AriaContext must have activeConstraints as unmodifiable list
- ExecutionStatus enum: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, BLOCKED
- AcesBlockedException: include String archaeologyUrl field

Flyway migrations — Oracle 23ai SQL dialect:
- Use VARCHAR2, NUMBER, CLOB, TIMESTAMP, VECTOR types
- Every table must have: tenant_id VARCHAR2(100) NOT NULL
- Every table must have: created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
- ACES_AUDIT_LOG must have a trigger preventing UPDATE and DELETE
- ACES_MEMORY_LTM must have: embedding VECTOR(1536) for Oracle AI Vector Search
- Add CREATE INDEX on tenant_id + created_at for all tables
- Add at end of each migration file a comment block:
  -- VPD POLICY (DBA RUNS THIS SEPARATELY):
  -- EXEC DBMS_RLS.ADD_POLICY(object_name=>'TABLE_NAME', policy_function=>'ACES_TENANT_POLICY')

Generate all exception classes with proper constructors and Oracle-safe messages.
```

---

## Engineer 6: DevOps Engineer — Project Scaffold + CI/CD + application.yml
**Estimated generation time:** 1 hour with Claude Code

### Files Claude Will Generate
```
/                              (project root)
├── pom.xml                    (parent multi-module)
├── CLAUDE.md                  (already exists — do not overwrite)
├── application.yml            (base config)
├── application-local.yml      (local dev overrides)
├── application-test.yml       (test config)
├── .github/
│   └── workflows/
│       └── aces-ci.yml        (GitHub Actions CI pipeline)
├── .claude/
│   └── settings.json          (Claude Code project settings)
└── scripts/
    ├── setup-dev.sh            (developer onboarding script)
    └── vault-local.sh          (local Vault token setup)
```

### Prompt to give Claude Code
```
Read CLAUDE.md first. Generate the project scaffold for ACES.

Parent pom.xml:
- GroupId: com.jpmc, ArtifactId: aces-platform, Version: 1.0.0-SNAPSHOT
- Java 21, Spring Boot 3.3.x, Spring AI 1.x
- Modules: aces-common, aces-api, aces-core, aces-mcp, aces-runtime, aces-governance, aces-observability
- Dependencies: oracle ojdbc11, HikariCP, Flyway Oracle, Spring Security OAuth2,
  Resilience4j Spring Boot Starter, Micrometer OTel Bridge,
  Spring Cloud Vault, Spring AI Core, Spring AI Anthropic, Spring AI OpenAI
- All versions locked in <properties> — no SNAPSHOT versions except aces itself
- Build plugins: maven-compiler-plugin (Java 21), spring-boot-maven-plugin,
  maven-surefire-plugin, checkstyle-plugin

application.yml:
- Exact Oracle config from CLAUDE.md section 4
- Spring AI config with ${OPENAI_API_KEY} and ${ANTHROPIC_API_KEY} from Vault
- Spring Security OAuth2 resource server with ${JWKS_URI}
- Actuator: expose health, info, metrics, prometheus
- Resilience4j circuit breaker config for "aria" instance
- aces.aria.base-url: ${ARIA_BASE_URL}
- aces.tool.max-iterations: 6
- aces.agent.revision-max: 1
- spring.cache.type: simple (ConcurrentHashMap — Phase 1)

application-local.yml:
- Log level DEBUG for com.jpmc.aces
- spring.jpa.show-sql: true
- Actuator: expose all endpoints

application-test.yml:
- H2 in Oracle compatibility mode for unit tests
- spring.jpa.hibernate.ddl-auto: create-drop
- Flyway: enabled: false (tests use Mockito for DB)

GitHub Actions CI:
- Java 21, Maven
- On: push to main and pull_request
- Steps: checkout, setup-java, mvn verify
- Fail fast on any test failure

setup-dev.sh:
- Check Java 21 installed
- Check Maven 3.9+ installed
- Check Vault CLI installed
- Print: "Connect to Oracle 23ai dev: get credentials from Vault at secret/aces/oracle/dev"
- mvn clean install -DskipTests to verify scaffold compiles
- Print success message
```

---

## Integration Day — After All 6 Modules Complete

Once all 6 engineers have generated and reviewed their modules:

### Step 1: Integration PR sequence
```
1. aces-common merged first (no dependencies on other modules)
2. aces-governance merged second (depends only on aces-common)
3. aces-mcp merged third (depends on aces-common)
4. aces-runtime merged fourth (depends on aces-common, aces-governance)
5. aces-core merged fifth (depends on aces-common, aces-mcp, aces-runtime)
6. aces-api merged last (depends on everything)
```

### Step 2: Integration test prompt for Claude Code
```
Read CLAUDE.md. The aces-platform project now has all modules merged.
Generate an end-to-end integration test:

AcesEndToEndIT.java in aces-api module:
- Spring Boot test with @SpringBootTest
- Test 1: Happy path — POST /api/v1/execute with mock ARIA returning ALLOW
  Assert: 200 response, executionId present, archaeology URL present
- Test 2: BLOCK path — mock ARIA returning BLOCK
  Assert: 403 response, decision: BLOCK, archaeology URL in response
- Test 3: Rate limit — 101 rapid requests to same tenant+tool
  Assert: first 100 return 200, 101st returns 429
- Mock external dependencies: ARIA REST API, LLM providers
- Use Oracle test instance or H2 Oracle-compatible mode for DB
```

### Step 3: Innovation Week demo prompt
```
Read CLAUDE.md. Generate an AcesInnovationWeekDemo.java class:

A @Component with a @PostConstruct that:
1. Registers a demo tool called "search-knowledge-base" in the tool registry
2. Registers the CopilotAgentAdapter as a demo agent
3. Submits a demo execution request for agentId "copilot-demo-v1"
4. Logs the full result including ARIA decision and agentReasoning
5. If ARIA blocks it (trust 0.30 on first run), logs the block reason

This gives us a live demo that works end-to-end on startup.
```

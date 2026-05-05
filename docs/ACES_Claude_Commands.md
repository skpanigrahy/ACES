# ACES Claude Code Commands
## Slash commands for common generation tasks

Place these in .claude/commands/ directory.
Engineers type /generate-service, /generate-test etc in Claude Code.

---

## /generate-service.md

Generate a complete Spring Boot @Service class for ACES.

Usage: /generate-service [ServiceName] [module] [purpose]
Example: /generate-service RateLimiterService aces-mcp "Token bucket rate limiting per tenant per tool"

Rules to follow from CLAUDE.md:
- Package: com.jpmc.aces.[module].[subpackage]
- Constructor injection only
- @WithSpan on all public methods
- Oracle via Spring Data JPA (no raw JDBC unless Vector Search)
- VPD: never add WHERE tenant_id manually
- SLF4J logging: private static final Logger log = LoggerFactory.getLogger([Class].class)
- All domain objects as records
- AcesException subclass for domain exceptions

Generate:
1. Main service class with full implementation
2. Domain records (request/response/result)
3. Domain exception class
4. Unit test with Mockito (happy path + failure path + edge case)
5. Relevant section of application.yml

---

## /generate-adapter.md

Generate a complete AgentPlugin adapter implementation.

Usage: /generate-adapter [AgentName] [providerType]
Example: /generate-adapter InternalRiskAgent internal

Rules from CLAUDE.md:
- Implements AgentPlugin interface exactly
- agentReasoning in AgentResponse is MANDATORY — never null, never empty
- Validates AriaContext.activeConstraints() before execution
- @WithSpan on execute() method
- Handles all ExecutionStatus values

Generate:
1. Adapter class implementing AgentPlugin
2. PluginManifest builder with correct layer, type, capabilities
3. execute() method with agentReasoning extraction
4. validateAriaConstraints() private method
5. Unit test verifying agentReasoning is never null
6. plugin.json manifest file

---

## /generate-migration.md

Generate a Flyway Oracle migration for a new ACES table.

Usage: /generate-migration [version] [table_name] [purpose]
Example: /generate-migration V9 ACES_WORKFLOWS "Stores workflow definitions and execution state"

Rules:
- Oracle 23ai SQL dialect (VARCHAR2, NUMBER, CLOB, TIMESTAMP, VECTOR)
- Always include tenant_id VARCHAR2(100) NOT NULL
- Always include created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
- Add index on (tenant_id, created_at)
- Add VPD policy comment at bottom (DBA runs separately)
- ACES_AUDIT_LOG gets UPDATE/DELETE prevention trigger

Generate:
1. Full CREATE TABLE statement
2. All indexes
3. Any sequences needed
4. VPD policy comment block
5. Rollback script (V[version]__rollback_[table].sql)

---

## /generate-module-spec.md

Generate a complete module spec file for a new ACES module.

Usage: /generate-module-spec [module-name] [purpose]
Example: /generate-module-spec aces-workflow "Multi-step workflow execution engine"

Generate a spec file following the pattern in specs/ALL_MODULE_ASSIGNMENTS.md:
- List all files to generate
- Describe each class's responsibility
- List dependencies (constructor-injected)
- List Oracle tables used
- List metrics to emit
- List test cases required
- List what NOT to generate

---

## /review-module.md

Review a generated module for CLAUDE.md compliance.

Usage: /review-module [module-name]
Example: /review-module aces-mcp

Check every file in the module for:
1. Package naming follows com.jpmc.aces.[module].* convention
2. No @Autowired field injection anywhere
3. All public service methods have @WithSpan
4. No hardcoded credentials, URLs, or hostnames
5. No WHERE tenant_id in any repository method
6. All AgentResponse instances have non-null agentReasoning
7. All Oracle entities have tenant_id column
8. All Flyway migrations have tenant_id column
9. ARIA check present before any agent execution
10. ARIA report sent after any agent execution
11. No Redis, Qdrant, Elasticsearch, Kafka imports
12. No wallet file references

Report: PASS or list of violations with file:line reference.

---

## /integration-test.md

Generate an end-to-end integration test for a completed module.

Usage: /integration-test [module-name]
Example: /integration-test aces-mcp

Generate:
1. @SpringBootTest integration test class
2. Oracle test connection setup (use test application.yml)
3. Happy path test
4. Failure path test
5. Tenant isolation test (two tenants, verify data separation)
6. Performance baseline test (100 requests, measure p99 latency)

# Skill: Oracle VPD Pattern for ACES
## Reusable pattern — Claude applies this to every Oracle interaction

When generating any class that reads from or writes to Oracle in ACES,
apply this pattern exactly. Never deviate.

---

## The Pattern

### 1. TenantContextHolder (already in aces-mcp)

```java
package com.jpmc.aces.mcp.tenant;

public final class TenantContextHolder {
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void setTenantId(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String getTenantId() {
        String tenantId = TENANT.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AcesTenantException("No tenant context set on current thread");
        }
        return tenantId;
    }

    public static void clear() {
        TENANT.remove();
    }
}
```

### 2. TenantGuardInterceptor sets VPD context on every request

```java
@Component
public class TenantGuardInterceptor implements HandlerInterceptor {

    private final JdbcTemplate jdbcTemplate;

    public TenantGuardInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String tenantId = extractTenantFromJwt(request);
        TenantContextHolder.setTenantId(tenantId);

        // Set Oracle VPD context — THIS is what makes all queries tenant-scoped
        jdbcTemplate.execute(
            "BEGIN DBMS_SESSION.SET_CONTEXT('ACES_CTX', 'TENANT_ID', ?); END;",
            (PreparedStatementCallback<Void>) ps -> {
                ps.setString(1, tenantId);
                ps.execute();
                return null;
            }
        );
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,
                                Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}
```

### 3. JPA Repository — NO tenant_id in queries

```java
// CORRECT — VPD handles tenant filtering automatically
@Repository
public interface ToolRegistryRepository extends JpaRepository<ToolRegistryEntity, String> {
    Optional<ToolRegistryEntity> findByToolName(String toolName);
    List<ToolRegistryEntity> findByStatus(String status);
    // No findByToolNameAndTenantId — VPD makes this unnecessary and wrong
}

// WRONG — do not do this
Optional<ToolRegistryEntity> findByToolNameAndTenantId(String name, String tenantId);
```

### 4. Oracle Entity — always include tenant_id column

```java
@Entity
@Table(name = "ACES_TOOL_REGISTRY")
public class ToolRegistryEntity {

    @Id
    @Column(name = "TOOL_ID")
    private String toolId;

    @Column(name = "TOOL_NAME", nullable = false)
    private String toolName;

    @Column(name = "TENANT_ID", nullable = false)
    private String tenantId; // stored but NOT used in queries — VPD handles it

    @Column(name = "RATE_LIMIT_PER_MINUTE")
    private int rateLimitPerMinute;

    @Column(name = "SCHEMA_JSON", columnDefinition = "CLOB")
    private String schemaJson;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "CREATED_AT", updatable = false)
    private Instant createdAt;
}
```

### 5. Oracle Vector Search — exact SQL pattern

```java
// In MemoryService — for Oracle AI Vector Search (LTM)
@Autowired
private JdbcTemplate jdbcTemplate;

public List<MemoryEntry> findSimilar(float[] queryEmbedding, int topK) {
    String sql = """
        SELECT memory_id, content, agent_id,
               VECTOR_DISTANCE(embedding, :queryVector, COSINE) AS distance
        FROM aces_memory_ltm
        WHERE agent_id = :agentId
        ORDER BY distance ASC
        FETCH FIRST :topK ROWS ONLY
        """;
    // tenant_id filter is applied by VPD automatically
    return jdbcTemplate.query(sql,
        new MapSqlParameterSource()
            .addValue("queryVector", queryEmbedding)
            .addValue("agentId", TenantContextHolder.getAgentId())
            .addValue("topK", topK),
        (rs, rowNum) -> new MemoryEntry(
            rs.getString("memory_id"),
            rs.getString("content"),
            rs.getDouble("distance")
        )
    );
}
```

### 6. Oracle AQ — for ARIAReporter async publishing

```java
@Configuration
public class OracleAQConfig {

    @Bean
    public ConnectionFactory oracleAQConnectionFactory(DataSource dataSource) {
        return AQjmsFactory.getConnectionFactory(dataSource);
    }

    @Bean
    public JmsTemplate ariaReportJmsTemplate(ConnectionFactory oracleAQConnectionFactory) {
        JmsTemplate template = new JmsTemplate(oracleAQConnectionFactory);
        template.setDefaultDestinationName("ACES_EXECUTION_REPORTS_QUEUE");
        template.setDeliveryPersistent(true);
        return template;
    }
}

// ARIAReporter usage
@Service
public class ARIAReporter {

    private final JmsTemplate ariaReportJmsTemplate;

    @Async("ariaReporterExecutor")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void reportAsync(ExecutionRecord record) {
        ariaReportJmsTemplate.convertAndSend(record);
        log.info("ExecutionRecord published to Oracle AQ: executionId={}",
            record.executionId());
    }
}
```

---

## VPD Setup SQL (DBA Runs This — Not Application Code)

```sql
-- 1. Create the context namespace
BEGIN
  DBMS_SESSION.CREATE_CONTEXT(
    namespace => 'ACES_CTX',
    accessed_globally => FALSE
  );
END;
/

-- 2. Create the policy function
CREATE OR REPLACE FUNCTION aces_tenant_policy(
  schema_name IN VARCHAR2,
  table_name  IN VARCHAR2
) RETURN VARCHAR2 AS
BEGIN
  RETURN 'TENANT_ID = SYS_CONTEXT(''ACES_CTX'', ''TENANT_ID'')';
END;
/

-- 3. Apply to each table (repeat for each ACES table)
BEGIN
  DBMS_RLS.ADD_POLICY(
    object_schema   => 'ACES',
    object_name     => 'ACES_TOOL_REGISTRY',
    policy_name     => 'ACES_TENANT_POLICY',
    function_schema => 'ACES',
    policy_function => 'ACES_TENANT_POLICY',
    statement_types => 'SELECT, INSERT, UPDATE, DELETE'
  );
END;
/
```

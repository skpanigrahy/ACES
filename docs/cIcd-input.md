# 📋 CI/CD PIPELINE USE CASE: SDD SPECIFICATION
*ACES Skill Definition Document for Jules/Jenkins Integration*

Since your team has chosen the **CI/CD pipeline use case** as the first implementation, here is the complete **SDD (Skill Definition Document)** specification, including inputs, processing logic, outputs, and Jules/Jenkins integration patterns.

---

## 🎯 USE CASE SCENARIO: BUILD FAILURE AUTO-REMEDIATION

**Problem:** A CI/CD pipeline fails due to a known, fixable issue (e.g., dependency version mismatch, linting error, test flake).
**ACES Solution:** Detect failure → Analyze root cause → Generate fix → Re-trigger pipeline → Verify success.

---

## 1️⃣ SDD INPUT REQUIREMENTS (What Triggers the Skill)

### **Signal Payload (From Jules/Jenkins Webhook)**
```json
{
  "type": "pipeline_failure",
  "source": "jules",
  "seal": "payments",
  "application": "payment-gateway",
  "pipeline": {
    "id": "jenkins-pipeline-4821",
    "name": "payment-gateway-build",
    "branch": "feature/upgrade-deps",
    "commit": "a1b2c3d4",
    "stage": "test",
    "error": {
      "code": "TEST_FAILURE",
      "message": "NullPointerException in PaymentServiceTest",
      "stack_trace": "..."
    }
  },
  "context": {
    "environment": "non-prod",
    "priority": "medium",
    "owner": "payments-team"
  },
  "timestamp": "2026-05-05T14:30:00Z"
}
```

### **Required Input Fields for SDD**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | String | ✅ | Event type: `pipeline_failure`, `build_timeout`, `dependency_conflict` |
| `source` | String | ✅ | Source system: `jules`, `jenkins`, `github-actions` |
| `seal` | String | ✅ | Team/ownership boundary for governance |
| `application` | String | ✅ | Target application name |
| `pipeline.id` | String | ✅ | Unique pipeline identifier |
| `pipeline.stage` | String | ✅ | Failed stage: `build`, `test`, `deploy` |
| `error.code` | String | ✅ | Categorized error code for rule matching |
| `context.environment` | String | ✅ | `prod`/`non-prod` for policy enforcement |

---

## 2️⃣ SDD STRUCTURE (Skill Definition Document)

### **File: `fix-pipeline-failure.skills`**
```yaml
skill: fix-pipeline-failure
version: 1.0
description: "Auto-remediate common CI/CD pipeline failures"

# Trigger conditions (when this skill can be invoked)
trigger:
  type: pipeline_failure
  error_codes:
    - TEST_FAILURE
    - DEPENDENCY_CONFLICT
    - LINT_ERROR
    - BUILD_TIMEOUT
  environments:
    - non-prod
    - staging

# Input parameters (validated before execution)
inputs:
  seal: string
  application: string
  pipeline:
    id: string
    branch: string
    commit: string
  error:
    code: string
    message: string

# Execution steps (orchestrated by ACES)
steps:
  - name: analyze_failure
    type: CHECK
    description: "Analyze failure root cause"
    action:
      type: agent
      agent: "failure-analyzer"
      input:
        error_code: "${error.code}"
        stack_trace: "${error.stack_trace}"
        pipeline_logs: "${pipeline.logs_url}"
    output:
      root_cause: string
      suggested_fix: string
      confidence: number

  - name: validate_fix_feasibility
    type: CHECK
    description: "Check if fix can be auto-applied"
    action:
      type: rule
      rules:
        - if: "${analyze_failure.confidence} > 0.8"
          then: "proceed"
        - if: "${context.environment} == 'prod'"
          then: "require_approval"
    output:
      fix_allowed: boolean
      approval_required: boolean

  - name: generate_fix
    type: ACTION
    description: "Generate code/config fix"
    action:
      type: agent
      agent: "code-fix-generator"
      input:
        root_cause: "${analyze_failure.root_cause}"
        suggested_fix: "${analyze_failure.suggested_fix}"
        repo_url: "${pipeline.repo_url}"
        branch: "${pipeline.branch}"
    output:
      fix_patch: string
      commit_message: string

  - name: apply_fix
    type: ACTION
    description: "Apply fix via Jules/Jenkins"
    action:
      type: plugin
      plugin: "jules-adapter"
      method: "create_fix_pr"
      parameters:
        repo: "${pipeline.repo_url}"
        branch: "${pipeline.branch}"
        patch: "${generate_fix.fix_patch}"
        commit_message: "${generate_fix.commit_message}"
        reviewer: "${context.owner}"
    output:
      pr_id: string
      pr_url: string

  - name: retrigger_pipeline
    type: ACTION
    description: "Re-trigger pipeline with fix"
    action:
      type: plugin
      plugin: "jules-adapter"
      method: "trigger_pipeline"
      parameters:
        pipeline_id: "${pipeline.id}"
        branch: "${pipeline.branch}"
        commit: "${apply_fix.new_commit}"
    output:
      new_pipeline_id: string
      status: string

  - name: verify_success
    type: VERIFICATION
    description: "Confirm pipeline passes with fix"
    action:
      type: plugin
      plugin: "jules-adapter"
      method: "poll_pipeline_status"
      parameters:
        pipeline_id: "${retrigger_pipeline.new_pipeline_id}"
        timeout_minutes: 30
    output:
      final_status: string
      success: boolean

# Governance policies (enforced before execution)
policies:
  - name: environment_restriction
    rule: "if environment == 'prod' then require_approval == true"
  
  - name: error_code_whitelist
    rule: "error.code must be in [TEST_FAILURE, DEPENDENCY_CONFLICT, LINT_ERROR]"
  
  - name: confidence_threshold
    rule: "analyze_failure.confidence must be >= 0.8"

# Constraints (execution limits)
constraints:
  retry_limit: 2
  timeout_minutes: 45
  max_fix_size_kb: 100
  approval_timeout_hours: 4

# Verification criteria (success conditions)
verification:
  success_condition: "${verify_success.success} == true"
  failure_actions:
    - notify_owner
    - escalate_to_engineer
```

---

## 3️⃣ PROCESSING LOGIC (What ACES Does With the SDD)

### **Step-by-Step Execution Flow**
```
1. SIGNAL RECEIVED
   ↓
2. SDD LOADED & VALIDATED
   • Check trigger conditions match
   • Validate input parameters
   • Load skill definition from registry
   ↓
3. GOVERNANCE CHECK
   • Validate seal ownership
   • Check environment policies
   • Verify error code whitelist
   • Determine if approval required
   ↓
4. STEP ORCHESTRATION (Sequential Execution)
   ↓
   4a. ANALYZE_FAILURE
       • Call failure-analyzer agent
       • Parse logs, stack trace, error code
       • Output: root cause + suggested fix + confidence
       ↓
   4b. VALIDATE_FIX_FEASIBILITY
       • Apply rule-based checks
       • Determine if auto-fix allowed
       • Flag if approval needed
       ↓
   4c. GENERATE_FIX (If allowed)
       • Call code-fix-generator agent
       • Create patch/commit for fix
       • Output: fix_patch + commit_message
       ↓
   4d. APPLY_FIX
       • Invoke jules-adapter plugin
       • Create PR with fix patch
       • Assign reviewer from context.owner
       ↓
   4e. RETRIGGER_PIPELINE
       • Call jules-adapter to trigger new build
       • Pass new commit/branch
       • Capture new pipeline ID
       ↓
   4f. VERIFY_SUCCESS
       • Poll pipeline status via jules-adapter
       • Wait for completion (with timeout)
       • Return final status
       ↓
5. OUTCOME EVALUATION
   • If success: Mark workflow COMPLETED
   • If failure: Retry (up to retry_limit) or ESCALATE
   ↓
6. AUDIT & OBSERVABILITY
   • Log all steps to audit_log table
   • Track execution time, cost, tokens
   • Emit metrics to Dynatrace
```

### **Key Processing Components**
| Component | Responsibility | Technology |
|-----------|---------------|------------|
| **Skill Loader** | Parse YAML, validate schema, load into memory | SnakeYAML, Jackson |
| **Rule Engine** | Evaluate policy constraints, trigger conditions | Drools, or custom Java rules |
| **Agent Executor** | Invoke AI agents for analysis/fix generation | Spring AI, LangChain4j |
| **Plugin Manager** | Route to Jules/Jenkins adapter, handle API calls | REST Client, WebClient |
| **State Tracker** | Maintain workflow state, handle retries | Oracle 23ai, JPA |
| **Audit Logger** | Record every step for compliance | Structured JSON logging |

---

## 4️⃣ OUTPUT & EXPECTED RESULTS

### **Success Output (JSON)**
```json
{
  "workflow_id": "wf-2026-05-05-abc123",
  "correlation_id": "ACES-PIPELINE-4821",
  "status": "COMPLETED",
  "skill": "fix-pipeline-failure",
  "steps_executed": [
    {
      "name": "analyze_failure",
      "status": "SUCCESS",
      "output": {
        "root_cause": "Null pointer in PaymentServiceTest.mockPayment()",
        "suggested_fix": "Add null check before calling mockPayment()",
        "confidence": 0.92
      }
    },
    {
      "name": "validate_fix_feasibility",
      "status": "SUCCESS",
      "output": {
        "fix_allowed": true,
        "approval_required": false
      }
    },
    {
      "name": "generate_fix",
      "status": "SUCCESS",
      "output": {
        "fix_patch": "@@ -45,6 +45,8 @@\n+ if (payment == null) return;\n...",
        "commit_message": "fix: add null check in PaymentServiceTest"
      }
    },
    {
      "name": "apply_fix",
      "status": "SUCCESS",
      "output": {
        "pr_id": "PR-8821",
        "pr_url": "https://git.jpmc.com/payments/payment-gateway/pull/8821"
      }
    },
    {
      "name": "retrigger_pipeline",
      "status": "SUCCESS",
      "output": {
        "new_pipeline_id": "jenkins-pipeline-4822",
        "status": "RUNNING"
      }
    },
    {
      "name": "verify_success",
      "status": "SUCCESS",
      "output": {
        "final_status": "SUCCESS",
        "success": true
      }
    }
  ],
  "execution_time_seconds": 342,
  "cost": {
    "infra_cost_usd": 0.12,
    "token_usage": 1450
  },
  "audit_trail": "https://audit.jpmc.com/aces/wf-2026-05-05-abc123"
}
```

### **Failure Output (With Retry/Escalation)**
```json
{
  "workflow_id": "wf-2026-05-05-abc123",
  "status": "ESCALATED",
  "failure_reason": "Pipeline still failed after fix application",
  "retry_count": 2,
  "escalation": {
    "notified": ["payments-team@jpmc.com"],
    "ticket_id": "INC-9921",
    "message": "Auto-fix failed for pipeline-4821. Manual intervention required."
  }
}
```

---

## 5️⃣ JULES/JENKINS INTEGRATION PATTERN

### **Plugin Adapter: `JulesAdapter.java`**
```java
@Component
public class JulesAdapter implements PluginAdapter {
    
    @Value("${jules.api.base-url}")
    private String julesBaseUrl;
    
    @Autowired
    private WebClient webClient;
    
    // Create PR with fix patch
    public JulesResponse createFixPR(FixRequest request) {
        return webClient.post()
            .uri(julesBaseUrl + "/api/v1/pr/create")
            .header("Authorization", "Bearer ${JULES_TOKEN}")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JulesResponse.class)
            .block();
    }
    
    // Trigger pipeline execution
    public JulesResponse triggerPipeline(PipelineTriggerRequest request) {
        return webClient.post()
            .uri(julesBaseUrl + "/api/v1/pipeline/trigger")
            .header("Authorization", "Bearer ${JULES_TOKEN}")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JulesResponse.class)
            .block();
    }
    
    // Poll pipeline status
    public PipelineStatus pollStatus(String pipelineId, Duration timeout) {
        // Implement polling with timeout
        // Return final status: SUCCESS, FAILED, TIMEOUT
    }
}
```

### **Configuration: `application.yml`**
```yaml
jules:
  api:
    base-url: https://jules.jpmc.internal
    timeout-seconds: 30
    retry:
      max-attempts: 3
      backoff-ms: 1000
  auth:
    token: ${JULES_API_TOKEN}
    refresh-endpoint: /oauth/token
```

### **Error Handling & Resilience**
```java
@Retryable(
    value = {ResourceAccessException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
@CircuitBreaker(name = "julesAdapter", fallbackMethod = "fallbackResponse")
public JulesResponse safeCall(Supplier<JulesResponse> call) {
    return call.get();
}

public JulesResponse fallbackResponse(ResourceAccessException ex) {
    log.warn("Jules API call failed, using fallback", ex);
    return JulesResponse.fallback("Service temporarily unavailable");
}
```

---

## 6️⃣ EXAMPLE: END-TO-END FLOW (Build Failure Auto-Fix)

### **Scenario Timeline**
```
T+0s: Jenkins build fails → Jules sends webhook to ACES
T+2s: ACES ingests signal, loads fix-pipeline-failure skill
T+5s: Governance check passes (non-prod, allowed error code)
T+8s: failure-analyzer agent identifies root cause (null pointer)
T+12s: Rule engine validates fix feasibility (confidence 0.92 > 0.8)
T+15s: code-fix-generator creates patch + commit message
T+20s: jules-adapter creates PR #8821 with fix
T+25s: jules-adapter re-triggers pipeline with new commit
T+300s: Pipeline completes successfully
T+302s: ACES verifies success, marks workflow COMPLETED
T+305s: Audit log persisted, metrics emitted to Dynatrace
```

### **Key Metrics Captured**
| Metric | Value | Purpose |
|--------|-------|---------|
| `workflow.duration_seconds` | 305 | Track automation speed |
| `pipeline.fix_success_rate` | 92% | Measure skill effectiveness |
| `agent.token_usage` | 1450 | Monitor AI cost |
| `jules.api.latency_ms` | 245 | Monitor integration health |
| `governance.approval_required` | false | Track policy enforcement |

---

## ✅ IMPLEMENTATION CHECKLIST

### **Phase 1: Foundation (Week 1)**
- [ ] Create `common-models` with `PipelineSignal`, `FixRequest`, `JulesResponse` DTOs
- [ ] Implement `JulesAdapter` plugin with mock responses for testing
- [ ] Define SDD schema validator (YAML → Java object mapping)
- [ ] Setup Oracle 23ai tables: `workflow_state`, `audit_log`, `skill_registry`

### **Phase 2: Core Logic (Week 2)**
- [ ] Implement `SkillLoader` service (parse YAML, validate inputs)
- [ ] Build `RuleEngine` for policy evaluation (start with simple Java rules)
- [ ] Create `AgentExecutor` stubs for `failure-analyzer` and `code-fix-generator`
- [ ] Implement `WorkflowOrchestrator` step execution loop

### **Phase 3: Integration & Demo (Week 3)**
- [ ] Connect `JulesAdapter` to real Jules API (non-prod environment)
- [ ] Add retry/circuit breaker logic for external calls
- [ ] Implement audit logging to Oracle 23ai
- [ ] Create Postman collection for end-to-end testing

### **Phase 4: Governance & Observability (Week 4)**
- [ ] Add policy enforcement layer (environment checks, approval workflows)
- [ ] Integrate Dynatrace for metrics/tracing
- [ ] Build simple dashboard view for workflow status
- [ ] Document runbook for escalation/failure handling

---

## 🎯 KEY DESIGN PRINCIPLES

1. **Declarative, Not Imperative**: SDD defines WHAT to do, not HOW. ACES orchestrates the how.
2. **Governance-First**: Every execution path validates policies before acting.
3. **Seal-Aware**: Execution respects team boundaries; no cross-seal actions without explicit approval.
4. **Closed-Loop Verification**: No workflow completes without confirming the desired outcome.
5. **Observability by Default**: Every step is logged, traced, and measurable.
6. **Fail-Safe**: If automation fails, escalate gracefully—never leave the system in an unknown state.

---

## 🚀 NEXT STEPS FOR YOUR TEAM

1. **Review this SDD spec** with the CI/CD team to validate error codes, Jules API endpoints, and policy rules.
2. **Start with mock implementations** of `failure-analyzer` and `code-fix-generator` agents to unblock orchestration logic.
3. **Use the JulesAdapter stub** to simulate PR creation and pipeline triggering until real API access is granted.
4. **Focus on one error code first** (e.g., `TEST_FAILURE`) to prove the end-to-end flow before expanding.

**You now have a production-ready SDD specification for the CI/CD use case.** This aligns with your ACES architecture, leverages your existing Jules/Jenkins infrastructure, and provides a clear path to implementation.

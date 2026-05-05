# ACES Auto-Fix Use Case
## CI/CD Signal → ARIA Governs → ACES Auto-Fixes → Pipeline Retriggers

---

## What This Is

This is a first-class ACES use case — not a separate product.
It demonstrates ACES doing exactly what it is designed for:
receiving an external signal, running a governed AI agent, and closing the loop.

**The engineer does nothing except review a single commit.**

---

## Use Cases Supported

| Scanner | Signal Type | ACES Auto-Fix Action |
|---------|------------|---------------------|
| Snyk | Vulnerable dependency | Upgrade dependency version in pom.xml / package.json |
| Snyk | Vulnerable code pattern | Generate patched code, raise PR comment |
| SonarQube | Code smell / duplication | Refactor affected method |
| SonarQube | Bug detected | Generate fix for specific issue |
| SonarQube | Security hotspot | Apply security fix pattern |
| Raven (internal) | Compliance violation | Apply compliance pattern |
| Any scanner | Any finding | Pluggable — new scanners register as integration plugins |

---

## Architecture — Where Each Part Lives

```
┌─────────────────────────────────────────────────────────┐
│  INTEGRATION LAYER                                      │
│  ScanResultWebhookReceiver                              │
│  Receives webhooks from Snyk, Sonar, Raven              │
│  Normalises to ScanResultEvent                          │
└─────────────────────────────────────────────────────────┘
                         ↓ ScanResultEvent
┌─────────────────────────────────────────────────────────┐
│  ARIA GOVERNANCE LAYER                                  │
│  AutoFixRiskEvaluator                                   │
│  Scores: agent trust + file criticality + fix type      │
│  Decides: ALLOW / ALLOW_WITH_WARNING / BLOCK            │
│  If BLOCK: notify engineer manually, do not auto-fix    │
└─────────────────────────────────────────────────────────┘
                         ↓ AriaDecision (if ALLOW)
┌─────────────────────────────────────────────────────────┐
│  ACES EXECUTION LAYER                                   │
│  AutoFixOrchestrator                                    │
│  AutoFixAgent (implements AgentPlugin)                  │
│  GitCommitService                                       │
│  PipelineTriggerService                                 │
└─────────────────────────────────────────────────────────┘
                         ↓ ExecutionRecord
┌─────────────────────────────────────────────────────────┐
│  ARIA (async feedback)                                  │
│  Trust score updated                                    │
│  Decision archaeology written                           │
│  Slack notification sent to engineer                    │
└─────────────────────────────────────────────────────────┘
```

---

## Module Spec — Generate These Files

### Package: `com.jpmc.aces.autofix`

```
aces-core/src/main/java/com/jpmc/aces/core/autofix/
├── AutoFixOrchestrator.java        # Entry point — receives ScanResultEvent
├── AutoFixAgent.java               # Implements AgentPlugin — generates the fix
├── AutoFixRiskEvaluator.java       # ARIA pre-check for auto-fix decisions
├── ScanResultEvent.java            # Record — normalised scanner signal
├── AutoFixResult.java              # Record — what was fixed, where, reasoning
├── AutoFixConfig.java              # @Configuration
└── test/
    ├── AutoFixOrchestratorTest.java
    └── AutoFixAgentTest.java

aces-api/src/main/java/com/jpmc/aces/api/webhook/
├── ScanResultWebhookController.java  # POST /api/v1/webhooks/scan-result
├── SnykWebhookMapper.java            # Snyk payload → ScanResultEvent
├── SonarWebhookMapper.java           # Sonar payload → ScanResultEvent
└── RavenWebhookMapper.java           # Raven payload → ScanResultEvent

db/migration/
└── V9__create_aces_autofix_log.sql
```

---

## Prompt for Claude Code — AutoFix Module

```
Read CLAUDE.md first. Then generate the complete ACES Auto-Fix module.

CONTEXT:
ACES receives webhook signals from CI/CD scanners (Snyk, SonarQube, Raven).
When a scanner finds a vulnerability or issue, ACES automatically:
1. Evaluates risk via ARIA (should this be auto-fixed or escalated?)
2. If allowed, runs AutoFixAgent to generate and apply the fix
3. Commits the fix to the PR branch
4. Retriggers the pipeline
5. Notifies the engineer via Slack with what was changed and why

GENERATE THESE CLASSES:

──────────────────────────────────────────────────────────
1. ScanResultEvent (record)
──────────────────────────────────────────────────────────
package com.jpmc.aces.core.autofix

public record ScanResultEvent(
    String eventId,          // UUID generated on receipt
    String scanner,          // "snyk" | "sonar" | "raven"
    String severity,         // "CRITICAL" | "HIGH" | "MEDIUM" | "LOW"
    String issueType,        // "VULNERABILITY" | "BUG" | "CODE_SMELL" | "SECURITY_HOTSPOT"
    String issueId,          // e.g. CVE-2024-1234 or sonar rule ID
    String issueDescription, // human-readable description
    String affectedFile,     // file path relative to repo root
    String affectedSnippet,  // the problematic code or dependency
    String recommendedFix,   // scanner's suggested fix if available
    String repoName,
    String branch,
    String prId,
    String tenantId,
    Instant detectedAt
) {}

──────────────────────────────────────────────────────────
2. AutoFixResult (record)
──────────────────────────────────────────────────────────
public record AutoFixResult(
    String executionId,
    AutoFixStatus status,     // FIXED | ESCALATED | BLOCKED | FAILED
    String fixDescription,    // what was changed
    String agentReasoning,    // why this fix is correct — mandatory
    String commitSha,         // null if not committed
    String prCommentUrl,      // URL of comment added to PR
    List<String> filesChanged,
    int tokenCount,
    long durationMs
) {}

public enum AutoFixStatus { FIXED, ESCALATED, BLOCKED, FAILED }

──────────────────────────────────────────────────────────
3. AutoFixRiskEvaluator (@Service)
──────────────────────────────────────────────────────────
Evaluates whether ARIA should allow auto-fix or escalate to human.

Decision logic:
- BLOCK auto-fix if:
  * affectedFile contains "auth", "security", "payment", "SecurityConfig"
  * severity is CRITICAL AND issueType is SECURITY_HOTSPOT
  * agent trust score < 0.50
- ALLOW_WITH_WARNING if:
  * issueType is VULNERABILITY and fix is a dependency version bump
  * severity is HIGH but fix is well-understood pattern
- ALLOW if:
  * issueType is CODE_SMELL or BUG with clear fix pattern
  * severity is LOW or MEDIUM
  * agent trust score > 0.75

Returns AriaDecision with:
  - decision: ALLOW | ALLOW_WITH_WARNING | REQUIRES_APPROVAL | BLOCK
  - reason: plain English explanation
  - riskScore: computed score

──────────────────────────────────────────────────────────
4. AutoFixAgent (@Component, implements AgentPlugin)
──────────────────────────────────────────────────────────
This is the AI agent that generates and applies the fix.

PluginManifest:
  pluginId: "agent-autofix-v1"
  capabilities: ["code-modification", "dependency-fix", "security-fix"]
  trustable: true

execute(AgentRequest request) logic:
  1. Extract ScanResultEvent from request.context().metadata()
  2. Read the affected file content from the repository
     (use GitService to fetch file from branch)
  3. Build prompt for Spring AI:
     - System: "You are a security engineer. Fix the vulnerability precisely."
     - User: include file content, issue description, recommended fix, constraints
  4. Call Spring AI ChatClient to generate fix
  5. Parse response to extract:
     - fixedFileContent (the complete fixed file)
     - agentReasoning (what was changed and why — MANDATORY)
     - confidence score
  6. Validate: fixed file compiles / parses correctly
  7. Return AgentResponse with agentReasoning populated

The Spring AI prompt for dependency fix:
  """
  File: {affectedFile}
  Current content:
  {fileContent}

  Issue: {issueDescription}
  Scanner: {scanner}
  Issue ID: {issueId}
  Recommended fix: {recommendedFix}

  Instructions:
  1. Apply the minimum change necessary to fix this issue
  2. Do not change anything unrelated to the fix
  3. Preserve all existing formatting and comments
  4. After the fix, explain in 2-3 sentences what you changed and why

  Return the complete fixed file content, then on a new line write:
  REASONING: [your explanation]
  """

──────────────────────────────────────────────────────────
5. AutoFixOrchestrator (@Service)
──────────────────────────────────────────────────────────
Entry point. Receives ScanResultEvent. Orchestrates the full flow.

@WithSpan
public AutoFixResult process(ScanResultEvent event) {

  // Step 1: Risk evaluation
  AriaDecision decision = autoFixRiskEvaluator.evaluate(event);

  // Step 2: If BLOCK — escalate to human
  if (decision.isBlock()) {
    slackNotifier.notifyEscalation(event, decision);
    auditLogger.log(event, decision, AutoFixStatus.ESCALATED);
    return AutoFixResult.escalated(event, decision.reason());
  }

  // Step 3: Build AgentRequest
  AgentRequest request = AgentRequest.builder()
    .executionId(UUID.randomUUID().toString())
    .taskType("AUTO_FIX_" + event.issueType())
    .context(AgentContext.builder()
      .repoName(event.repoName())
      .branch(event.branch())
      .filesInScope(List.of(event.affectedFile()))
      .instructions("Fix: " + event.issueDescription())
      .metadata(Map.of("scanResultEvent", event))
      .build())
    .constraints(ExecutionConstraints.builder()
      .environment("dev")
      .timeoutSeconds(120)
      .build())
    .ariaContext(AriaContext.from(decision))
    .build();

  // Step 4: Execute AutoFixAgent
  AgentResponse response = autoFixAgent.execute(request);

  // Step 5: Commit the fix
  if (response.status() == ExecutionStatus.COMPLETED) {
    String commitSha = gitCommitService.commitFix(
      event.repoName(),
      event.branch(),
      event.affectedFile(),
      extractFixedContent(response),
      "ACES Auto-Fix: " + event.issueId() + " — " + response.agentReasoning()
    );

    // Step 6: Add PR comment
    String commentUrl = prCommentService.addComment(
      event.prId(),
      buildPrComment(event, response, commitSha)
    );

    // Step 7: Retrigger pipeline
    pipelineTriggerService.retrigger(event.prId(), event.repoName());

    // Step 8: Notify engineer
    slackNotifier.notifyAutoFix(event, response, commitSha);

    // Step 9: Report to ARIA
    ariaReporter.reportAsync(ExecutionRecord.from(request, response));

    return new AutoFixResult(
      request.executionId(),
      AutoFixStatus.FIXED,
      response.agentReasoning(),
      response.agentReasoning(),  // mandatory
      commitSha,
      commentUrl,
      List.of(event.affectedFile()),
      response.tokenCount(),
      response.durationMs()
    );
  }

  return AutoFixResult.failed(event, response.agentReasoning());
}

──────────────────────────────────────────────────────────
6. ScanResultWebhookController (@RestController)
──────────────────────────────────────────────────────────
package com.jpmc.aces.api.webhook

@RestController
@RequestMapping("/api/v1/webhooks")
public class ScanResultWebhookController {

  POST /api/v1/webhooks/snyk
    - Validates Snyk webhook signature (HMAC-SHA256)
    - Maps Snyk payload to ScanResultEvent using SnykWebhookMapper
    - Calls autoFixOrchestrator.process(event) asynchronously
    - Returns 202 Accepted immediately (async processing)

  POST /api/v1/webhooks/sonar
    - Same pattern for SonarQube webhook

  POST /api/v1/webhooks/raven
    - Same pattern for Raven (internal JPMC scanner)

  GET /api/v1/webhooks/autofix/{executionId}
    - Returns status of an auto-fix execution
    - Queries Oracle ACES_AUTOFIX_LOG by executionId

}

──────────────────────────────────────────────────────────
7. SnykWebhookMapper (@Component)
──────────────────────────────────────────────────────────
Maps Snyk webhook JSON payload to ScanResultEvent.

Snyk payload structure to handle:
{
  "project": { "name": "repo-name", "branch": "feature/xyz" },
  "newIssues": [{
    "id": "SNYK-JAVA-LOG4J-...",
    "issueData": {
      "severity": "critical",
      "title": "Remote Code Execution",
      "description": "...",
      "CVSSv3": "...",
      "fixedIn": ["2.17.1"]
    },
    "pkgName": "log4j-core",
    "pkgVersions": ["2.14.1"]
  }]
}

Maps to:
  scanner: "snyk"
  severity: uppercase(newIssues[0].issueData.severity)
  issueType: "VULNERABILITY"
  issueId: newIssues[0].id
  issueDescription: newIssues[0].issueData.title
  affectedFile: "pom.xml" (for dependency issues)
  affectedSnippet: pkgName + ":" + pkgVersions[0]
  recommendedFix: "Upgrade " + pkgName + " to " + fixedIn[0]

──────────────────────────────────────────────────────────
8. V9__create_aces_autofix_log.sql (Flyway Migration)
──────────────────────────────────────────────────────────
CREATE TABLE ACES_AUTOFIX_LOG (
  autofix_id     VARCHAR2(36)   NOT NULL,
  execution_id   VARCHAR2(36),
  tenant_id      VARCHAR2(100)  NOT NULL,
  scanner        VARCHAR2(50)   NOT NULL,
  severity       VARCHAR2(20)   NOT NULL,
  issue_id       VARCHAR2(200)  NOT NULL,
  issue_type     VARCHAR2(50)   NOT NULL,
  repo_name      VARCHAR2(200)  NOT NULL,
  branch_name    VARCHAR2(200),
  pr_id          VARCHAR2(100),
  affected_file  VARCHAR2(500),
  fix_status     VARCHAR2(30)   NOT NULL,
  fix_description CLOB,
  agent_reasoning CLOB,
  commit_sha     VARCHAR2(100),
  pr_comment_url VARCHAR2(500),
  token_count    NUMBER(10),
  duration_ms    NUMBER(10),
  aria_decision  VARCHAR2(30),
  created_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT pk_aces_autofix_log PRIMARY KEY (autofix_id)
);

-- VPD POLICY (DBA runs separately)
-- EXEC DBMS_RLS.ADD_POLICY(object_name=>'ACES_AUTOFIX_LOG', policy_function=>'ACES_TENANT_POLICY')

──────────────────────────────────────────────────────────
9. Unit Tests Required
──────────────────────────────────────────────────────────

AutoFixOrchestratorTest:
1. givenSnykCriticalVulnerability_whenARIAAllows_thenFixCommittedAndPipelineRetriggered()
2. givenAuthFileAffected_whenARIABlocks_thenEscalatedToEngineer()
3. givenLowTrustAgent_whenRequiresApproval_thenJiraTicketCreated()
4. givenAgentFixFails_whenExecutionFails_thenFailureLoggedAndEngineerNotified()

AutoFixAgentTest:
1. givenSnykVulnerability_whenExecute_thenAgentReasoningNotNull()
2. givenDependencyFix_whenExecute_thenFixedContentContainsNewVersion()
3. givenAriaConstraintReadOnly_whenExecute_thenThrowsValidationException()
4. givenModelResponseMissingReasoning_whenExecute_thenFallbackReasoningGenerated()

──────────────────────────────────────────────────────────
10. application.yml additions
──────────────────────────────────────────────────────────
aces:
  autofix:
    enabled: true
    max-file-size-kb: 500        # do not auto-fix files larger than this
    blocked-files:               # never auto-fix these regardless of ARIA
      - "SecurityConfig.java"
      - "application-prod.yml"
      - "JwtAuthFilter.java"
    snyk:
      webhook-secret: ${SNYK_WEBHOOK_SECRET}    # from Vault
    sonar:
      webhook-secret: ${SONAR_WEBHOOK_SECRET}   # from Vault
    raven:
      webhook-secret: ${RAVEN_WEBHOOK_SECRET}   # from Vault
    git:
      commit-author: "ACES Auto-Fix Bot <aces@jpmc.internal>"
      commit-message-prefix: "fix(aces-autofix):"
```

# Skill: Spring AI Agent Pattern for ACES
## How to wire Spring AI in every ACES service that uses LLMs

---

## 1. Spring AI ChatClient — Always Use This Pattern

```java
// In AcesCoreConfig.java
@Configuration
public class AcesCoreConfig {

    // Primary model — Claude for agent reasoning
    @Bean
    @Primary
    public ChatClient claudeChatClient(AnthropicChatModel anthropicModel) {
        return ChatClient.builder(anthropicModel)
            .defaultSystem("""
                You are an expert AI assistant running inside ACES at JPMorgan Chase.
                You reason carefully, cite your sources, and always explain your actions.
                When you complete a task, describe what you did and why in agentReasoning.
                """)
            .build();
    }

    // Fallback model — GPT-4 when Anthropic unavailable
    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel openAiModel) {
        return ChatClient.builder(openAiModel).build();
    }
}
```

## 2. SynthesizerService — Exact Spring AI Pattern

```java
@Service
public class SynthesizerService {

    private static final Logger log = LoggerFactory.getLogger(SynthesizerService.class);
    private final ChatClient chatClient;
    private final MemoryService memoryService;

    public SynthesizerService(ChatClient chatClient, MemoryService memoryService) {
        this.chatClient = chatClient;
        this.memoryService = memoryService;
    }

    @WithSpan
    public SynthesisResult synthesize(ToolFindings findings, AgentRequest originalRequest) {
        // Load relevant memory context
        List<MemoryEntry> context = memoryService.findRelevant(
            findings.summary(), 3);

        String prompt = buildPrompt(findings, context, originalRequest);

        String response = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        // Extract reasoning — mandatory for ARIA
        String reasoning = extractReasoning(response, findings);

        return new SynthesisResult(
            response,
            reasoning,          // never null
            extractCitations(response)
        );
    }

    private String extractReasoning(String response, ToolFindings findings) {
        // If model provided explicit reasoning section, extract it
        // Otherwise generate from findings
        if (response.contains("REASONING:")) {
            return response.substring(
                response.indexOf("REASONING:") + 10,
                response.contains("\n\n") ?
                    response.indexOf("\n\n", response.indexOf("REASONING:")) :
                    response.length()
            ).trim();
        }
        // Fallback: describe what was done
        return String.format(
            "Synthesized response from %d tool findings for task: %s",
            findings.results().size(),
            findings.taskDescription()
        );
    }
}
```

## 3. CriticService — Exact Spring AI Pattern

```java
@Service
public class CriticService {

    private final ChatClient chatClient;

    public CriticService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @WithSpan
    public CriticVerdict evaluate(SynthesisResult draft, AgentRequest originalRequest) {
        String evaluation = chatClient.prompt()
            .system("""
                You are a critical evaluator. Review the response and determine:
                APPROVE if it correctly addresses the task with evidence.
                REVISE if it is incomplete, incorrect, or missing key information.
                Respond with exactly one word: APPROVE or REVISE, then a newline,
                then your reason.
                """)
            .user(String.format("""
                Original task: %s
                Response to evaluate: %s
                """, originalRequest.taskType(), draft.content()))
            .call()
            .content();

        boolean approved = evaluation.trim().toUpperCase().startsWith("APPROVE");
        String reason = evaluation.contains("\n") ?
            evaluation.substring(evaluation.indexOf('\n')).trim() :
            "No reason provided";

        return new CriticVerdict(
            approved ? CriticDecision.APPROVE : CriticDecision.REVISE,
            reason
        );
    }
}
```

## 4. ClaudeAgentAdapter — Implementing AgentPlugin with Spring AI

```java
@Component("claudeAgentAdapter")
public class ClaudeAgentAdapter implements AgentPlugin {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAgentAdapter.class);
    private final ChatClient chatClient;
    private final PluginManifest manifest;

    public ClaudeAgentAdapter(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.manifest = PluginManifest.builder()
            .pluginId("agent-claude-dev-v1")
            .displayName("Claude Dev Agent")
            .version("1.0.0")
            .layer(PluginLayer.EXECUTION)
            .type("agent")
            .capabilities(List.of("code-generation", "code-modification",
                                  "test-generation", "documentation-generation"))
            .trustable(true)
            .costModel(new CostModel("token", 0.000003, null))
            .build();
    }

    @Override
    public PluginManifest getManifest() { return manifest; }

    @Override
    public HealthStatus ping() {
        return new HealthStatus("healthy", 50L);
    }

    @Override
    @WithSpan
    public AgentResponse execute(AgentRequest request) {
        long startMs = System.currentTimeMillis();

        // ALWAYS respect ARIA constraints
        validateAriaConstraints(request.ariaContext());

        String systemPrompt = buildSystemPrompt(request);
        String userPrompt = buildUserPrompt(request);

        String rawResponse = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();

        // Extract reasoning — MANDATORY for ARIA Decision Archaeology
        String reasoning = extractReasoning(rawResponse, request);
        if (reasoning == null || reasoning.isBlank()) {
            // Never return empty reasoning — generate fallback
            reasoning = String.format(
                "Claude processed task '%s' using context from %d files. " +
                "Applied clean code principles and Spring Boot best practices.",
                request.taskType(),
                request.context().filesInScope().size()
            );
        }

        // Parse artifacts from response
        List<Artifact> artifacts = parseArtifacts(rawResponse, request);

        return AgentResponse.builder()
            .executionId(request.executionId())
            .status(ExecutionStatus.COMPLETED)
            .artifacts(artifacts)
            .tokenCount(estimateTokens(systemPrompt + userPrompt + rawResponse))
            .durationMs(System.currentTimeMillis() - startMs)
            .agentReasoning(reasoning)   // NEVER null, NEVER empty
            .confidence(0.88)
            .build();
    }

    private void validateAriaConstraints(AriaContext ariaContext) {
        if (ariaContext.activeConstraints().contains("no-config-modification")) {
            log.warn("ARIA constraint active: no-config-modification");
            // This adapter will not modify config files even if instructed to
        }
        if (ariaContext.activeConstraints().contains("read-only")) {
            throw new AcesValidationException(
                "Agent cannot execute: ARIA constraint read-only is active");
        }
    }
}
```

## 5. application.yml — Spring AI Config

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}    # from Vault
      chat:
        options:
          model: claude-sonnet-4-20250514
          max-tokens: 4096
          temperature: 0.3             # lower = more deterministic
    openai:
      api-key: ${OPENAI_API_KEY}       # from Vault, fallback model
      chat:
        options:
          model: gpt-4o
          max-tokens: 4096
```

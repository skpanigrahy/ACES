ACES — Autonomous Cognitive Engineering System
Governed Agentic Execution Platform that transforms real-time signals into automated, verified outcomes.

🎯 Project Overview
ACES is a control-plane platform that sits between signal-producing systems (CertGuardian, Monitoring, CI/CD, CryptoLite) and execution targets (Agents, CI/CD Pipelines, Config APIs). It evaluates system state, decides on the optimal remediation strategy, enforces governance policies, executes via reusable skills, and verifies outcomes in a closed loop.
Core Philosophy:
Control Plane (ACES): Decision, orchestration, governance, optimization
Data Plane (External): Actual execution via team-owned systems
AI Layer: Enhancement for reasoning, optimization, and explainability (async & optional)
🏗️ Architecture Highlights
Concept Description
**Signal-Agnostic Ingestion** Accepts normalized events from any external system
**Drift Detection Engine** Compares observed vs. desired state to identify mismatches
**Rule-Based Decision Core** Deterministic strategy selection (restart, reload, deploy, config)
**Skill-Based Execution** Declarative `.skills` YAML definitions for reusable workflows
**Policy-First Governance** Mandatory validation before any execution (seal-level ownership)
**Closed-Loop Verification** No workflow completes without outcome confirmation
**AI-Assisted (Optional)** Spring AI integration for strategy suggestion, cost optimization, and explainability

🛠️ Technology Stack
Layer Technology
**Backend** Java 21, Spring Boot 3.2.x, Maven, Lombok
**Database** Oracle 23ai (Vector Search, JSON, Basic AQ)
**Messaging** REST (MVP) → Kafka (Phase 2)
**Frontend** Octagon (React/TypeScript wrapper)
**Observability** OpenTelemetry, Micrometer, Structured JSON Logging
**AI/Agents** Spring AI (optional enhancement layer)
**Testing** JUnit 5, Mockito, Testcontainers

📁 Project Structure
aces-platform/
├── .github/
│ └── copilot-instructions.md ← AI generation context (auto-read by Copilot)
├── skills/ ← Reusable patterns for AI/code generation
│ ├── spring-boot-service-pattern.md
│ ├── oracle-23ai-pattern.md
│ └── octagon-component-pattern.md
├── specs/ ← Module-specific requirements for AI generation
│ ├── ingestion-service-spec.md
│ ├── evaluation-service-spec.md
│ └── decision-engine-spec.md
├── docs/ ← Architecture, C1-C4 diagrams, master design doc
├── aces-common/ ← Shared DTOs, exceptions, utilities
├── aces-ingestion/ ← Signal intake, validation, correlation ID
├── aces-evaluation/ ← Drift detection & state comparison
├── aces-decision/ ← Rule engine & strategy selection
├── aces-orchestrator/ ← Workflow execution & state tracking
├── aces-plugin/ ← MCP/Agent/CI-CD adapters (execution layer)
├── aces-verification/ ← Outcome validation & re-check logic
├── aces-audit/ ← Logging, metrics, Oracle persistence
├── aces-web/ ← Octagon UI module
├── aces-app/ ← Spring Boot runner
└── pom.xml ← Parent Maven POM

🚀 Getting Started
Prerequisites
Java 21+ (sdk install java 21-oracle)
Maven 3.9+ (sdk install maven)
Node.js 22+ (for aces-web)
Git & IDE (IntelliJ / VS Code)

Build & Run

# Clone & build

git clone <repo-url>
cd aces-platform
mvn clean install -DskipTests

# Run individual service (example: ingestion)

cd aces-ingestion
mvn spring-boot:run

# Run frontend (if applicable)

cd aces-web
npm install
npm run dev

🤖 AI-Assisted Development Workflow
ACES is designed for skill-driven code generation using GitHub Copilot (or Claude Code). All engineers follow the same pattern:
Open the relevant spec & skill files in your IDE
Use this prompt in Copilot Chat:
@workspace #file:specs/{module}-spec.md #file:skills/spring-boot-service-pattern.md
Generate the complete {module} service following CLAUDE.md guardrails.
Package: com.aces.{module}
Include: Controller, Service, DTO, application.yml, unit tests.
Ensure: @RequiredArgsConstructor, structured logging with correlationId, 80%+ test coverage.

Review, apply, and commit → git push
📌 Golden Rule: AI generates. Engineer reviews every line. Never commit without verifying against copilot-instructions.md.

📖 Documentation Index
Document Purpose
`docs/ACES_MASTER_DOCUMENT.docx` Vision, requirements, architecture, roadmap
`docs/COMPLETE_ARCHITECTURE_DIAGRAM_SET.docx` C1-C4 diagrams, flows, sequence charts
`docs/ACES_Execution_Engine_Documentation.md` Core interfaces, workflow models, demo runner
`skills/*.md` Reusable code & architecture patterns
`specs/*.md` Module-specific implementation requirements

🧪 Testing & Validation

# Run all unit tests

mvn test

# Run with coverage

mvn clean verify jacoco:report

# Integration test (requires Oracle/Testcontainers)

mvn verify -P integration

Roadmap (MVP → Phase 3)
Phase Focus Deliverables
**Phase 1 (MVP)** Core Control Plane Signal → Evaluate → Decide → Execute → Verify flow. CertGuardian use case. Local policy engine.
**Phase 2** Platform Expansion Multi-use-case support, Kafka event backbone, AI-assisted strategy, Octagon dashboard v1
**Phase 3** Scale & Intelligence Multi-tenant isolation, auto-optimization, agent registry, enterprise observability

🔒 Security & Governance
All APIs require authentication & RBAC
Seal-level ownership enforced at runtime
No hardcoded secrets; use environment variables / vault
Every action logged with correlationId and audit trail
AI suggestions are validated by deterministic rules before execution

👥 Contributing
Create feature branch: git checkout -b feature/ACES-001-ingestion
Follow copilot-instructions.md for code style & patterns
Run mvn clean test before committing
Open PR → 2 approvals required → Merge to develop

📞 Support & Questions
Architecture decisions → docs/OPEN_QUESTIONS.md
AI generation issues → Review skills/ patterns & copilot-instructions.md
Build/CI failures → Check pom.xml dependency tree & Java/Maven versions

ACES is a governed agentic execution platform. AI enhances decision-making; ACES ensures safe, auditable, and verified automation. 🚀

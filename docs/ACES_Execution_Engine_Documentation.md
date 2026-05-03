# ACES Execution Engine Documentation

This document outlines the design and implementation of the ACES (Agentic Cloud Engineering Services) Execution Engine, integrated into the `certguardian-new-ui-poc-rest` module. The primary goal is to provide a flexible and extensible framework for orchestrating workflows involving various tools and agents.

## Project Structure

The ACES components are organized under the `com.jpmc.aces` package within the `certguardian-new-ui-poc-rest` module. The directory structure is as follows:

```
certguardian-new-ui-poc-rest/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── jpmc/
│                   └── aces/
│                       ├── AgentService.java
│                       ├── AgentResult.java
│                       ├── AcesDemoRunner.java
│                       ├── MockAgentService.java
│                       ├── MockRunLedger.java
│                       ├── MockToolGateway.java
│                       ├── Orchestrator.java
│                       ├── RunEvent.java
│                       ├── RunLedger.java
│                       ├── SimpleOrchestrator.java
│                       ├── ToolGateway.java
│                       ├── ToolRequest.java
│                       ├── ToolResult.java
│                       ├── WorkflowDefinition.java
│                       └── WorkflowStep.java
└── target/
    └── classes/ (Compiled .class files)
```

## Core Interfaces

Four core interfaces define the interaction points within the ACES Execution Engine:

1.  **`Orchestrator`**: Defines the contract for executing a workflow. The `execute` method takes a `WorkflowDefinition` and initial input, returning the final output of the workflow.
    ```java
    public interface Orchestrator {
        Object execute(WorkflowDefinition workflow, Map<String, Object> input);
    }
    ```

2.  **`ToolGateway`**: Provides an abstraction for interacting with external tools. The `execute` method takes a `ToolRequest` and returns a `ToolResult`.
    ```java
    public interface ToolGateway {
        ToolResult execute(ToolRequest request);
    }
    ```

3.  **`AgentService`**: Defines the interface for agent interactions. The `process` method takes an input string and returns an `AgentResult`.
    ```java
    public interface AgentService {
        AgentResult process(String input);
    }
    ```

4.  **`RunLedger`**: Responsible for logging events during workflow execution. The `log` method records a `RunEvent`.
    ```java
    public interface RunLedger {
        void log(RunEvent event);
    }
    ```

## Domain Models

Several domain models facilitate data exchange and workflow definition:

-   **`WorkflowDefinition`**: Represents a complete workflow, containing a name and a list of `WorkflowStep` objects.
-   **`WorkflowStep`**: Defines a single step within a workflow, including its ID, type (TOOL or AGENT), tool name (if applicable), and input parameters.
-   **`RunEvent`**: Captures details of each step's execution, such as step name, status, output, cost, and timestamp.
-   **`ToolRequest`**: Encapsulates the details required to invoke a tool, including the tool's name and its parameters.
-   **`ToolResult`**: Holds the outcome of a tool execution, indicating success, a message, and the tool's output.
-   **`AgentResult`**: Contains the results from an agent's processing, typically a summary and a plan.

## Implementations

### `SimpleOrchestrator`

This is the core of the execution engine. It implements the `Orchestrator` interface and is responsible for:

-   Accepting a `WorkflowDefinition` and initial input.
-   Executing steps sequentially.
-   Delegating to `ToolGateway` for `TOOL` type steps.
-   Delegating to `AgentService` for `AGENT` type steps.
-   Logging each step's execution using `RunLedger`.
-   Returning the final output of the workflow.

### Mock Implementations

For demonstration purposes, the following mock implementations are provided:

-   **`MockToolGateway`**: Simulates tool execution, returning predefined responses for "get-ci-logs" and "get-git-diff" tools.
-   **`MockAgentService`**: Provides a mock response for agent processing, returning a generic summary and plan based on the input.
-   **`MockRunLedger`**: Prints `RunEvent` details to the console, simulating a logging mechanism.

## How to Run the Demo

To run the provided demo, follow these steps:

1.  **Navigate to the project directory**: Ensure you are in the `certguardian-new-ui-poc-rest` directory.
2.  **Compile the Java files**: Execute the following command to compile all source files:
    ```bash
    find src/main/java/com/jpmc/aces -name "*.java" | xargs javac -d target/classes
    ```
3.  **Run the `AcesDemoRunner`**: Execute the main class to start the workflow:
    ```bash
    java -cp target/classes com.jpmc.aces.AcesDemoRunner
    ```

This will execute a workflow that first calls a mock tool to get CI logs and then passes the (mocked) output to a mock agent for analysis. The `MockRunLedger` will print the events to the console.

## Future Enhancements

This initial implementation provides a solid foundation. Future enhancements could include:

-   **Real Tool and Agent Integrations**: Replace mock implementations with actual calls to CI/CD systems, Git repositories, and AI agents.
-   **Error Handling and Retries**: Implement more robust error handling, including retry mechanisms for transient failures.
-   **Cost Tracking**: Enhance `RunEvent` and `RunLedger` to support detailed cost tracking for each step.
-   **Workflow Persistence**: Store workflow definitions and run events in a persistent storage (e.g., database).
-   **Asynchronous Execution**: Introduce asynchronous processing for long-running tool or agent operations.
-   **UI Integration**: Develop UI pages (Run View, Cost View) in `certguardian-new-ui-poc-web` to visualize workflows and their execution.

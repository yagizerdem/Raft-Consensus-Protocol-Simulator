# Raft-Consensus-Protocol-Simulator

## Table of Contents

- [Introduction](#introduction)
- [Features](#raft-protocol-features)
- [Architecture Overview](#architecture-overview)
- [Workflow / Algorithm](#workflow--algorithm)
- [Configuration](#configuration)
- [How to Run](#how-to-run)
- [Testing](#testing)
- [License](#license)

## Introduction

This project is a fully custom, dependency-free implementation of a distributed replica management and consensus system designed as an alternative to Paxos and inspired by Raft. Every subsystem is built entirely from scratch, with a strong emphasis on theoretical computer science and low-level control.

I developed a complete JSON serialization module, featuring a hand-written lexer, an LL(1) parser, and runtime type-checking using Java's reflection API. The system supports complex nested data structures, including arrays, ArrayLists, Hashtables, and generic Lists.

For networking, I implemented a custom RPC framework over raw TCP sockets, without relying on external libraries. This framework enables the consensus layer and replica coordination to operate across processes and machines. The consensus protocol itself is my own design "Raft-style" algorithm adapted for multi-process environments and engineered for clarity, determinism, and debuggability.

The entire system is heavily multi-threaded and multi-processed, pushing Java's concurrency primitives to their limits. Nearly every line of code is tied to a core concept in theoretical computer science, making the project both a technical showcase and a debugging challenge.

To improve visibility, I built a custom reflection-based debugging subsystem that exposes internal state across processes and forwards output streams to a replicated client shell. Through this shell, users can send actual OS-level commands (CMD) to the leader node, which propagates operations across the cluster.

For ease of deployment, I also created batch scripts that initialize and launch the entire distributed system with a single command.

## Raft Protocol Features

### Persistent State (Stored on Stable Storage)

- **currentTerm** - The latest term the server has seen, updated before responding to any RPC.
- **votedFor** - The candidate ID that received this server's vote in the current term.
- **log[]** - The replicated log; each entry contains a state-machine command and the term in which it was received by the leader.

### Volatile State (All Servers)

- **commitIndex** - Index of the highest log entry known to be committed.
- **lastApplied** - Index of the highest log entry applied to the state machine.

### Volatile State (Leaders Only)

- **nextIndex[]** - For each follower, the index of the next log entry to send (initially leader's last log index + 1).
- **matchIndex[]** - For each follower, the highest log entry known to be replicated on that follower.

---

## RequestVote RPC

### Arguments

- **term** - Candidate's current term.
- **candidateId** - The ID of the candidate requesting the vote.
- **lastLogIndex** - Index of the candidate's last log entry.
- **lastLogTerm** - Term of the candidate's last log entry.

### Results

- **term** - Receiver's current term for term updates.
- **voteGranted** - True if the receiver grants its vote.

### Receiver Behavior

1. Reject vote if the candidate's term is smaller than receiver's currentTerm.
2. Grant vote only if:
   - `votedFor` is null **or** matches candidateId,
   - and candidate's log is at least as up-to-date as receiver's log.

---

## AppendEntries RPC

### Arguments

- **term** - Leader's term.
- **leaderId** - Leader's ID (for redirection).
- **prevLogIndex** - Index of log entry preceding the new ones.
- **prevLogTerm** - Term of the prevLogIndex entry.
- **entries[]** - Log entries to store (empty for heartbeat).
- **leaderCommit** - Leader's commitIndex.

### Results

- **term** - Receiver's current term.
- **success** - True if follower contained matching prevLogIndex/prevLogTerm.

### Receiver Behavior

1. Reject if term < currentTerm.
2. Reject if log doesn't contain entry at prevLogIndex with matching term.
3. If conflicting entry exists, delete entry and all following entries.
4. Append any new entries not already in the log.
5. If leaderCommit > commitIndex, update `commitIndex = min(leaderCommit, lastNewEntryIndex)`.

---

## Server Rules

### All Servers

- If `commitIndex > lastApplied`:  
  Apply log entries to the state machine in order.
- If an RPC request or response contains a higher term:  
  Update own term and convert to follower.

---

## Follower Rules

- Respond to RequestVote and AppendEntries RPCs.
- If the election timeout elapses without receiving AppendEntries or granting a vote -> become Candidate.

---

## Candidate Rules

- On conversion to candidate:
  - Increment `currentTerm`.
  - Vote for self.
  - Reset election timer.
  - Send RequestVote RPCs to all other servers.
- If votes are received from a majority -> become Leader.
- If AppendEntries is received from a leader -> revert to Follower.
- If the election timeout elapses -> start a new election.

---

## Leader Rules

- Upon election:
  - Send initial empty AppendEntries RPCs (heartbeats).
  - Repeat periodically to maintain authority and prevent new elections.
- On client command:
  - Append command to the local log.
  - Replicate entry via AppendEntries.
  - Apply entry once committed.
- For each follower:
  - Send log entries starting at `nextIndex`.
  - If AppendEntries succeeds:
    - Update `nextIndex` and `matchIndex`.
  - If AppendEntries fails due to inconsistency:
    - Decrement `nextIndex` and retry.
- Commit rule:
  - If there exists an index `N` such that:
    - A majority of `matchIndex[] >= N`,
    - And `log[N].term == currentTerm`,
    - Then set `commitIndex = N`.

## Architecture Overview

The project is built on a modular Java architecture. Each core responsibility is isolated into its own module to improve clarity, maintainability, and testability.

---

## Modular Architecture

### 1. gRPC Module

- Handles all RPC communication.
- Uses a Node.js-like approach:
  - Every incoming gRPC call is processed in a **new Java thread**.
  - This provides non-blocking behavior at the application level.
- Concurrency control is managed explicitly within the application.

### 2. JSON Module

- Performs JSON serialization and deserialization for all DTOs.
- Uses a custom annotation system (`@JsonSerializable`, `@JsonElement`).
- Reflection-based and behaves as a lightweight custom JSON engine.
- Includes a simple **LL(1) parser and lexer** for JSON tokenization.
- Deserialization is implemented using a predictable LL(1) grammar.
- Parsed key-value pairs are stored in an internal **HashMap** before being mapped onto DTO fields.

### 3. Raft Module

- Contains the full Raft state machine logic.
- Manages elections, terms, leader/follower/candidate states.
- Clearly separates persistent state and volatile state.

### 4. Test Java Files

- Includes isolated test scenarios for each module.
- Uses port-based multi-instance simulations to emulate distributed behavior.
- Validates node logic consistency and interaction correctness.

---

## Java Object-Oriented Approach

- Each module adheres to the Single Responsibility Principle.
- State management is organized to remain thread safe and predictable.
- Modules are loosely coupled; communication happens strictly through DTOs.

---

## Dependency Resolution

The **JSON module** and **gRPC module** operate as fully independent layers.  
There is no direct coupling between them; each module handles its own concern with no shared state or cross-module assumptions.

The gRPC module mimics a **Node.js-style event loop**, but implemented with **Java multithreading**.  
Each incoming RPC creates a new worker thread, and the module triggers the corresponding interface method based on the RPC type.

The Raft module depends only on the **IRpcHandler interface**, not on the internal mechanics of gRPC.  
This keeps the transport layer isolated and makes the Raft logic testable and replaceable.

Example usage of dependency resolution and event dispatch:

```java
this.grpc = new Grpc(this.serverPort, new IRpcHandler() {
    @Override
    public void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto) {
        RaftModule.this.handleRequestVoteRpc(requestVoteDto);
    }

    @Override
    public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
        RaftModule.this.handleRequestVoteResponseRpc(requestVoteResponseDto);
    }

    @Override
    public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
        RaftModule.this.handleAppendEntriesRpc(appendEntriesDto);
    }

    @Override
    public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {

        RaftModule.this.handleAppendResponseRpc(appendEntriesResponseDto);
    }

    @Override
    public void handleClientCommandRpc(ClientCommandRPCDTO clientCommandDto) {
        RaftModule.this.handleClientCommandRpc(clientCommandDto);
    }

    @Override
    public void handleClientCommandResponseRpc(ClientCommandRPCResultDTO clientCommandRPCResultDTO) {
        throw new UnsupportedOperationException(); // server should not handle client response
    }
});
```

This pattern demonstrates:

- **Transport independence:** Raft never touches gRPC internals.
- **Loose coupling:** Only `IRpcHandler` is required.
- **Event-driven dispatch:** Each RPC maps directly to a handler callback.
- **Multithreaded execution:** Every RPC runs in its own thread.
- **Predictable control flow:** `RaftModule` handles pure logic; the gRPC module handles all I/O and concurrency.

This clean separation ensures:

- Tests can simulate RPC behavior without actual network calls.
- The Raft logic stays deterministic and isolated from transport-level concurrency noise.

## Workflow / Algorithm

The `Start()` function initializes the entire Raft module and brings all subsystems online.  
Once initialized, every RPC listener in the gRPC module operates on a dedicated thread.  
This event-driven, multi-threaded design ensures that Raft logic remains responsive and isolated from transport-level concurrency.

Access to the file-based persistent storage is synchronized carefully.  
All reads and writes are performed through Java’s stream I/O APIs using buffered operations, ensuring durability and preventing race conditions when multiple threads interact with the log or metadata files.

Despite relying on plain Java file I/O for persistence, the server can sustain approximately **200 RPC requests per second**, and can handle even heavier loads under stress tests.  
The lightweight architecture combined with efficient thread scheduling and minimal serialization overhead keeps latency low even at higher request rates.

By design, the Raft module separates the heartbeat mechanism from log replication:

- **Heartbeat messages** are sent at fixed intervals in their own background thread.
- **AppendEntries with actual log payload** are dispatched by another thread following the replication rules.

This strict separation of responsibilities different threads for heartbeats and for real log entries simplifies debugging and makes replication behavior easier to trace in logs.  
It also avoids accidental coupling between timing-sensitive heartbeat traffic and heavy log replication traffic.

Overall, this execution model provides a clean, deterministic Raft implementation where concurrency is pushed to the outer layers (RPC threads and background workers), while the Raft core itself stays structured, predictable, and easy to reason about.

## Configuration

To run the project, first **clone the Git repository** or download and unzip it.  
A valid **Java SDK** (JDK) installation is required to compile and execute the program.

Do not modify or relocate the directory structure after cloning or extracting the repository.  
Several scripts depend on the expected file paths, and breaking the structure will prevent the launcher from compiling or running the Raft server.

The project includes Windows `.bat` launcher scripts that:

1. Compile the Java source files.
2. Start the Raft server instances.
3. Launch the replicated shell client.

These scripts automate the full workflow—compilation, startup, and client connection—so you can run the entire system without manually invoking Java commands.  
As long as the repository structure remains intact, the launchers will correctly build and run the clustered Raft environment.

There are multiple launcher scripts included in the repository, each designed to simulate different Raft cluster configurations.

3NLauncher initilze raft server with 3 nodes in cluster and a replicated shell to send shell commands to leader server.

3NDefaultLauncher initilze raft server with 3 nodes in cluster and default shell commands (that shell commands create merge sort algorithm in java then compile & execute code) and a replicated shell to send shell commands to leader server.

The 3NDefaultLauncherWithBackpressure script is the same as 3NDefaultLauncher, but it executes the default shell commands without waiting for commit acknowledgments to be propagated back to the client shell. This acts as a stress test to verify that the Raft system remains consistent and preserves correct log ordering under backpressure.

5NLauncher same with 3NLauncher but starts 5 nodes in cluster

5NDefaultLauncher same with 3NDefaultLauncher but starts 5 nodes in cluster

5NDefaultLauncherWithBackpressure same with 3NDefaultLauncherWithBackpressure but starts 5 nodes in cluster

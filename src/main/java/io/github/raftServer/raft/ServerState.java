package io.github.raftServer.raft;

import io.github.raftServer.models.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerState {

    private int currentTerm = 0;

    // null means not voted in current term
    private String votedFor = null;

    // Raft log index starts at 1.
    // Java list index starts at 0, so log index N = log.get(N - 1)
    private final List<Log> log = new ArrayList<>();


    private int commitIndex = 0;
    private int lastApplied = 0;

    // Leader volatile state

    private final Map<String, Integer> nextIndex = new HashMap<>();
    private final Map<String, Integer> matchIndex = new HashMap<>();

    public synchronized int getCurrentTerm() {
        return currentTerm;
    }

    public synchronized void setCurrentTerm(int currentTerm) {
        if (currentTerm < this.currentTerm) {
            throw new IllegalArgumentException("currentTerm cannot decrease");
        }

        this.currentTerm = currentTerm;
    }

    public synchronized String getVotedFor() {
        return votedFor;
    }

    public synchronized void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public synchronized List<Log> getLogCopy() {
        return new ArrayList<>(log);
    }

    public synchronized int getLastLogIndex() {
        return log.size();
    }

    public synchronized int getLastLogTerm() {
        if (log.isEmpty()) return 0;
        return log.get(log.size() - 1).getTerm();
    }

    public synchronized Log getLogEntry(int index) {
        if (index <= 0 || index > log.size()) {
            throw new IndexOutOfBoundsException("Invalid Raft log index: " + index);
        }

        return log.get(index - 1);
    }

    public synchronized void appendLogEntry(Log entry) {
        log.add(entry);
    }

    public synchronized int getCommitIndex() {
        return commitIndex;
    }

    public synchronized void setCommitIndex(int commitIndex) {
        if (commitIndex < this.commitIndex) {
            throw new IllegalArgumentException("commitIndex cannot decrease");
        }

        this.commitIndex = commitIndex;
    }

    public synchronized int getLastApplied() {
        return lastApplied;
    }

    public synchronized void setLastApplied(int lastApplied) {
        if (lastApplied < this.lastApplied) {
            throw new IllegalArgumentException("lastApplied cannot decrease");
        }

        this.lastApplied = lastApplied;
    }

    public synchronized Map<String, Integer> getNextIndex() {
        return new HashMap<>(nextIndex);
    }

    public synchronized Map<String, Integer> getMatchIndex() {
        return new HashMap<>(matchIndex);
    }

    public synchronized void initializeLeaderState(List<String> peerIds) {
        nextIndex.clear();
        matchIndex.clear();

        int leaderLastLogIndex = getLastLogIndex();

        for (String peerId : peerIds) {
            nextIndex.put(peerId, leaderLastLogIndex + 1);
            matchIndex.put(peerId, 0);
        }
    }

    public synchronized int getNextIndex(String nodeId) {
        return nextIndex.getOrDefault(nodeId, 1);
    }

    public synchronized void setNextIndex(String nodeId, int index) {
        nextIndex.put(nodeId, index);
    }

    public synchronized int getMatchIndex(String nodeId) {
        return matchIndex.getOrDefault(nodeId, 0);
    }

    public synchronized void setMatchIndex(String nodeId, int index) {
        int old = matchIndex.getOrDefault(nodeId, 0);

        if (index < old) {
            throw new IllegalArgumentException("matchIndex cannot decrease");
        }

        matchIndex.put(nodeId, index);
    }
}
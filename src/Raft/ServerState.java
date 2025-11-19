package Raft;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;
import Models.Log;

import java.util.ArrayList;

@JsonSerializable
public class ServerState extends PersistentServerState {


    // Volatile state on all servers

    @JsonElement
    protected int commitIndex;
    @JsonElement
    protected int lastApplied;

    //Volatile state on leaders
    @JsonElement
    protected ArrayList<Integer> nextIndex = new ArrayList<>();
    @JsonElement
    protected  ArrayList<Integer> matchIndex = new ArrayList<>();
    @JsonElement

    protected String serverLevel = ServerLevel.Follower;

    public long getCurrentTerm() {
        return currentTerm;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public int getLastApplied() {
        return lastApplied;
    }

    public void setLastApplied(int lastApplied) {
        this.lastApplied = lastApplied;
    }

    public ArrayList<Integer> getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(ArrayList<Integer> nextIndex) {
        this.nextIndex = nextIndex;
    }

    public ArrayList<Integer> getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(ArrayList<Integer> matchIndex) {
        this.matchIndex = matchIndex;
    }

    public String getServerLevel() {
        return serverLevel;
    }

    public void setServerLevel(String serverLevel) {
        this.serverLevel = serverLevel;
    }

    public static ServerState GetDefaultServerState(){
        ServerState serverState = new ServerState();
        serverState.commitIndex = 0;
        serverState.lastApplied  = 0;
        serverState.currentTerm =0;
        serverState.votedFor = null;
        serverState.nextIndex = new ArrayList<>();
        serverState.matchIndex = new ArrayList<>();
        serverState.logs = new ArrayList<>();
        serverState.serverLevel = ServerLevel.Follower;

        return  serverState;
    }
}

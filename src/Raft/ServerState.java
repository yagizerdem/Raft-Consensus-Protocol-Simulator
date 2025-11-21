package Raft;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;
import Models.Log;

import java.util.ArrayList;
import java.util.Hashtable;

@JsonSerializable
public class ServerState extends PersistentServerState {


    // Volatile state on all servers

    @JsonElement
    protected int commitIndex;
    @JsonElement
    protected int lastApplied;

    //Volatile state on leaders
    @JsonElement
    protected Hashtable<Integer, Integer> nextIndex = new Hashtable<Integer, Integer>();
    @JsonElement
    protected  Hashtable<Integer, Integer>matchIndex = new Hashtable<Integer, Integer>();
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

    public Hashtable<Integer, Integer> getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(Hashtable<Integer, Integer> nextIndex) {
        this.nextIndex = nextIndex;
    }

    public Hashtable<Integer, Integer> getMatchIndex() {
        return matchIndex;
    }

    public void setMatchIndex(Hashtable<Integer, Integer> matchIndex) {
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
        serverState.nextIndex = new Hashtable<Integer, Integer>();
        serverState.matchIndex = new Hashtable<Integer, Integer>();
        serverState.serverLevel = ServerLevel.Follower;

        return  serverState;
    }
}

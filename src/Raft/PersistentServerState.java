package Raft;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;
import Models.Log;

import java.util.ArrayList;

@JsonSerializable
public class PersistentServerState {

    @JsonElement
    public long currentTerm;
    @JsonElement
    public String votedFor; // server ports used as unique identifier as server id's

    @JsonElement
    public ArrayList<Log> logs = new ArrayList<>();

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public ArrayList<Log> getLogs() {
        return logs;
    }

    public void setLogs(ArrayList<Log> logs) {
        this.logs = logs;
    }
}

package Models;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;

import java.util.ArrayList;

@JsonSerializable
public class AppendEntriesRPCDTO {

    @JsonElement
    public  long term;

    @JsonElement
    public String leaderId;

    @JsonElement
    public long prevLogIndex;
    @JsonElement
    public long prevLogTerm;

    @JsonElement
    public ArrayList<Log> entries;

    @JsonElement
    public long leaderCommit;

    public AppendEntriesRPCDTO(){
        entries = new ArrayList<>();
    }


    @Override
    public String toString() {
        return "AppendEntriesRPCDTO{" +
                "term=" + term +
                ", leaderId='" + leaderId + '\'' +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", leaderCommit=" + leaderCommit +
                ", entries=" + (entries != null ? entries.toString() : "null") +
                '}';
    }
}

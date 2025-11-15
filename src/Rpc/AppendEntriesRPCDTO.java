package Rpc;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;
import State.Log;

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


    public ArrayList<Log> entries;

    @JsonElement
    public long leaderCommit;

    public AppendEntriesRPCDTO(){}
}

package Models;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;

@JsonSerializable
public class RpcPayload {

    // AppendEntries
    @JsonElement
    public AppendEntriesRPCDTO appendEntriesRPCDTO;

    // RequestVote
    @JsonElement
    public RequestVoteRPCDTO requestVoteRPCDTO;

    // AppendEntriesResponse
    @JsonElement
    public AppendEntriesRPCResultDTO appendEntriesRPCResultDTO;

    // RequestVoteResponse
    @JsonElement
    public RequestVoteResultRPCDTO requestVoteResultRPCDTO;

    @JsonElement
    public String type;

    @Override
    public String toString() {
        return "RpcPayload{" +
                "type='" + type + '\'' +
                ", appendEntriesRPCDTO=" + appendEntriesRPCDTO +
                ", requestVoteRPCDTO=" + requestVoteRPCDTO +
                ", appendEntriesRPCResultDTO=" + appendEntriesRPCResultDTO +
                ", requestVoteResultRPCDTO=" + requestVoteResultRPCDTO +
                '}';
    }
}

package io.github.raftServer.models;

import io.github.raftServer.jsonModule.*;

@JsonSerializable
public class RequestVoteResultRPCDTO {
    @JsonElement
    public long term;
    @JsonElement
    public boolean voteGranted;

    @JsonElement
    public String traceId;

    public RequestVoteResultRPCDTO() {
    }

    public RequestVoteResultRPCDTO(long term, Boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    @Override
    public String toString() {
        return "RequestVoteResultRPCDTO{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                ", traceId=" + traceId +
                '}';
    }
}

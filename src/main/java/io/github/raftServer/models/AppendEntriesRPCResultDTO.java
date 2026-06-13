package io.github.raftServer.models;

import io.github.raftServer.jsonModule.*;

@JsonSerializable
public class AppendEntriesRPCResultDTO {

    @JsonElement
    public long term;
    @JsonElement
    public boolean success;

    @JsonElement
    public String traceId;

    public AppendEntriesRPCResultDTO() {
    }

    public AppendEntriesRPCResultDTO(long term, Boolean success) {
        this.term = term;
        this.success = success;
    }

    @Override
    public String toString() {
        return "{ \"AppendEntriesRPCResultDTO\": {" +
                "\"term\": " + term +
                ", \"success\": " + success +
                ", \"traceId\" . " + traceId +
                "} }";
    }



}

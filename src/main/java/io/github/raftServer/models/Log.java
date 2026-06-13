package io.github.raftServer.models;

import io.github.raftServer.jsonModule.*;

@JsonSerializable
public class Log {
    @JsonElement
    public long index;
    @JsonElement
    public long term;
    @JsonElement
    public String shellCommand;

}

package io.github.raftServer.models;

import io.github.raftServer.jsonModule.*;

@JsonSerializable
public class ClientCommandRPCDTO {

    @JsonElement
    public Integer clientPort;

    @JsonElement
    public String shellCommand;

    @Override
    public String toString() {
        return "ClientCommandRPCDTO{" +
                "clientPort=" + clientPort +
                ", shellCommand='" + shellCommand + '\'' +
                '}';
    }

}

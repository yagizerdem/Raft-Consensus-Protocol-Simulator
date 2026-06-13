package io.github.raftServer.models;

import io.github.raftServer.jsonModule.JsonElement;
import io.github.raftServer.jsonModule.JsonSerializable;

@JsonSerializable
public class ServerCredentialsDTO {
    @JsonElement
    public String nodeId;

    @JsonElement
    public int port;


    public ServerCredentialsDTO() {}

    public ServerCredentialsDTO(String nodeId, int port) {
        this.nodeId = nodeId;
        this.port = port;
    }

    @Override
    public String toString() {
        return "ServerCredentialsDTO{" +
                "nodeId=" + nodeId +
                ", port=" + port +
                '}';
    }
}

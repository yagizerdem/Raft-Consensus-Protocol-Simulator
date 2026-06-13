package io.github.raftServer.models;

import io.github.raftServer.jsonModule.*;
@JsonSerializable
public class ClientCommandRPCResultDTO {

    @JsonElement
    private int commitIndex;

    @JsonElement
    private boolean success;

    @JsonElement
    private String message;


    public int getCommitIndex() {
        return commitIndex;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }


    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

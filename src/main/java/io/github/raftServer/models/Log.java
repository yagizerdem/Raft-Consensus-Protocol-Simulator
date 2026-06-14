package io.github.raftServer.models;

import io.github.raftServer.jsonModule.*;

@JsonSerializable
public class Log {
    @JsonElement
    public int index;
    @JsonElement
    public int term;
    @JsonElement
    public String shellCommand;

    public int getIndex() {
        return index;
    }

    public int getTerm() {
        return term;
    }

    public String getShellCommand() {
        return shellCommand;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setShellCommand(String shellCommand) {
        this.shellCommand = shellCommand;
    }

    @Override
    public String toString() {
        return "Log{index=" + index +
                ", term=" + term +
                ", shellCommand='" + shellCommand + "'}";
    }
}

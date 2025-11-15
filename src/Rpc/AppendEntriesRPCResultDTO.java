package Rpc;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;

@JsonSerializable
public class AppendEntriesRPCResultDTO {

    @JsonElement
    public long term;
    @JsonElement
    public boolean success;


    public AppendEntriesRPCResultDTO() {
    }

    public AppendEntriesRPCResultDTO(long term, Boolean success) {
        this.term = term;
        this.success = success;
    }

    @Override
    public String toString() {
        return "AppendEntriesRPCResultDTO{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }



}

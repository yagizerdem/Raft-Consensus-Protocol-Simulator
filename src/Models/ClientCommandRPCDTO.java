package Models;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;

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

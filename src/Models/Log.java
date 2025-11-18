package Models;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;

@JsonSerializable
public class Log {
    @JsonElement
    public long index;

    @JsonElement
    public long term;

    @JsonElement
    public String text;


}

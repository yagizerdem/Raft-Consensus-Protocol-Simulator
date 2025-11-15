package State;

import JsonModule.JsonElement;
import JsonModule.JsonSerializable;

@JsonSerializable
public class Log {
    @JsonElement
    public long index;

    @JsonElement
    public String text;


}

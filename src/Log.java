import java.security.PublicKey;

@JsonSerializable
public class Log {
    @JsonElement
    public long index;

    @JsonElement
    public String text;


}

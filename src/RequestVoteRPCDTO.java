@JsonSerializable
public class RequestVoteRPCDTO {

    @JsonElement
    public long term;
    @JsonElement
    public String candidateId;
    @JsonElement
    public long lastLogIndex;
    @JsonElement
    public long lastLogTerm;

    public RequestVoteRPCDTO() {
    }

    public RequestVoteRPCDTO(long term, String candidateId, long lastLogIndex, long lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "RequestVoteRPCDTO{" +
                "term=" + term +
                ", candidateId='" + candidateId + '\'' +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
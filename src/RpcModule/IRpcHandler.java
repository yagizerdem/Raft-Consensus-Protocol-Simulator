package RpcModule;

import Models.AppendEntriesRPCDTO;
import Models.AppendEntriesRPCResultDTO;
import Models.RequestVoteRPCDTO;
import Models.RequestVoteResultRPCDTO;

public interface IRpcHandler {

    void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto);

    void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto);

    void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto);

    void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto);
}
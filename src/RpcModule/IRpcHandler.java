package RpcModule;

import Models.*;

public interface IRpcHandler {

    void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto);

    void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto);

    void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto);

    void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto);

    void handleClientCommandRpc(ClientCommandRPCDTO clientCommandDto);
}
package io.github.raftServer.rpcModule;

import io.github.raftServer.models.*;


public interface IRpcHandler {

    void handleRequestVoteRpc(Grpc.RpcConnection connection, RequestVoteRPCDTO requestVoteDto);

    void handleRequestVoteResponseRpc(Grpc.RpcConnection connection, RequestVoteResultRPCDTO requestVoteResponseDto);

    void handleAppendEntriesRpc(Grpc.RpcConnection connection, AppendEntriesRPCDTO appendEntriesDto);

    void handleAppendEntriesResponseRpc(Grpc.RpcConnection connection, AppendEntriesRPCResultDTO appendEntriesResponseDto);

    void handleClientCommandRpc(Grpc.RpcConnection connection, ClientCommandRPCDTO clientCommandDto);

    void handleClientCommandResponseRpc(Grpc.RpcConnection connection, ClientCommandRPCResultDTO clientCommandRPCResultDTO);

    void  handleServerCredentialsRpc(Grpc.RpcConnection connection, ServerCredentialsDTO serverCredentialsDTO);

    void  handleServerCredentialsResponseRpc(Grpc.RpcConnection connection, ServerCredentialsResponseDTO serverCredentialsResponseDTO);
}
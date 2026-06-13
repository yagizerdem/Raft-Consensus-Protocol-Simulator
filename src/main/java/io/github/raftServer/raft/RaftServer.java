package io.github.raftServer.raft;

import io.github.raftServer.models.*;
import io.github.raftServer.rpcModule.Grpc;
import io.github.raftServer.rpcModule.IRpcHandler;

import java.util.ArrayList;
import java.util.List;

public class RaftServer implements IRpcHandler {

    public Grpc grpc;

    public List<Grpc.Peer> peers = new ArrayList<>();

    public String nodeId;

    public int serverPort;

    public RaftServer(int port, List<Grpc.Peer> peers, String nodeId){
        this.serverPort = port;
        this.peers = peers;
        this.nodeId = nodeId;
        this.grpc = new Grpc(port, peers, nodeId, this);
    }

    public void start() {
        // listen connections
        this.grpc.start();
        this.sendServerCredentials();
    }

    public void sendServerCredentials() {
        new Thread(() ->{
            while (true) {
                try {
                    ServerCredentialsDTO dto = new ServerCredentialsDTO();
                    dto.nodeId = this.nodeId;
                    dto.port = this.serverPort;
                    for(Grpc.Peer peer : this.peers) {
                        this.grpc.sendServerCredentialsRpc(peer.nodeId(), dto);
                    }
                    Thread.sleep(5000);
                }catch (InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }).start();
    }

    @Override
    public void handleRequestVoteRpc(Grpc.RpcConnection connection, RequestVoteRPCDTO requestVoteDto) {
    }

    @Override
    public void handleRequestVoteResponseRpc(Grpc.RpcConnection connection, RequestVoteResultRPCDTO requestVoteResponseDto) {
    }

    @Override
    public void handleAppendEntriesRpc(Grpc.RpcConnection connection, AppendEntriesRPCDTO appendEntriesDto) {
    }

    @Override
    public void handleAppendEntriesResponseRpc(Grpc.RpcConnection connection, AppendEntriesRPCResultDTO appendEntriesResponseDto) {
    }

    @Override
    public void handleClientCommandRpc(Grpc.RpcConnection connection, ClientCommandRPCDTO clientCommandDto) {
    }

    @Override
    public void handleClientCommandResponseRpc(Grpc.RpcConnection connection, ClientCommandRPCResultDTO clientCommandRPCResultDTO) {
    }

    @Override
    public void handleServerCredentialsRpc(Grpc.RpcConnection connection, ServerCredentialsDTO serverCredentialsDTO) {}

    @Override
    public void handleServerCredentialsResponseRpc(Grpc.RpcConnection connection, ServerCredentialsResponseDTO serverCredentialsResponseDTO) {

    }
}

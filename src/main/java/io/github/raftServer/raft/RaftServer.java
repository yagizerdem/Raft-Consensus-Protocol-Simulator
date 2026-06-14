package io.github.raftServer.raft;

import io.github.raftServer.models.*;
import io.github.raftServer.rpcModule.Grpc;
import io.github.raftServer.rpcModule.IRpcHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RaftServer implements IRpcHandler {

    public Grpc grpc;

    public List<Grpc.Peer> peers = new ArrayList<>();

    public String nodeId;

    public int serverPort;

    public double randomTimer;

    public int TIME_INTERVAL = 50;

    public String leaderNodeId = null;

    public ServerLevel serverLevel = ServerLevel.FOLLOWER;

    public int currentTerm;

    public String votedFor;

    private final Storage storage = new Storage();

    public RaftServer(int port, List<Grpc.Peer> peers, String nodeId){
        this.serverPort = port;
        this.peers = peers;
        this.nodeId = nodeId;
        this.grpc = new Grpc(port, peers, nodeId, this);
        this.randomTimer = 5 + Math.random() * 5;
    }

    public void start() {
        storage.initFiles();
        this.loadPersistentState();
        // listen connections
        this.grpc.start();
        this.sendServerCredentials();
        this.countDown();
        this.startElection();

        votedFor = storage.readVotedFor();
        currentTerm = storage.readCurrentTerm();
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
                    Thread.sleep((int)(TIME_INTERVAL * 100));
                }catch (InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }).start();
    }

    public void countDown() {
        new Thread(() ->{
            while (true) {
                try {
                    Thread.sleep((int)TIME_INTERVAL);
                    this.randomTimer = Math.max(this.randomTimer - (int)TIME_INTERVAL, 0);
                }catch (InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }).start();
    }

    public void startElection() {
        new Thread(() -> {
            while (true) {
                try {
                    if(this.randomTimer == 0 && this.serverLevel.equals(ServerLevel.FOLLOWER)) {
                        this.serverLevel = ServerLevel.CANDIDATE;
                        RequestVoteRPCDTO dto = new RequestVoteRPCDTO();
                        dto.candidateId = this.nodeId;
                        dto.term = this.currentTerm;
                    }
                    Thread.sleep(TIME_INTERVAL);
                }catch (InterruptedException ex) {
                    System.err.println("Election timer thread was interrupted while waiting for the next tick.");
                }
            }
        }).start();
    }

    public void loadPersistentState() {
        this.currentTerm = this.storage.readCurrentTerm();
        this.votedFor = this.storage.readVotedFor();
    }

    public boolean isLeader() {
        if(this.leaderNodeId == null) return false;
        return  this.leaderNodeId.equals(this.nodeId);
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

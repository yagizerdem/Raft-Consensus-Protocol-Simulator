package io.github.raftServer.raft;

import com.sun.jdi.event.ThreadStartEvent;
import io.github.raftServer.models.ServerCredentialsDTO;
import io.github.raftServer.rpcModule.Grpc;

import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;

public class RaftServer {

    public Grpc grpc;

    public List<Grpc.Peer> peers = new ArrayList<>();

    public String nodeId;

    public int serverPort;

    public RaftServer(int port, List<Grpc.Peer> peers, String nodeId){
        this.serverPort = port;
        this.peers = peers;
        this.nodeId = nodeId;
        this.grpc = new Grpc(port, peers, nodeId);
    }

    public void start() {
        // listen connections
        this.grpc.start();
        this.sendServerCredentials();
    }

    public void sendServerCredentials(){
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
}

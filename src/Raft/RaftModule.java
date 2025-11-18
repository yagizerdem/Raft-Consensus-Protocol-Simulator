package Raft;

import Models.AppendEntriesRPCDTO;
import Models.AppendEntriesRPCResultDTO;
import Models.RequestVoteRPCDTO;
import Models.RequestVoteResultRPCDTO;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;

public class RaftModule {

    public int serverPort;
    private Grpc grpc;

    public  RaftModule(int serverPort){
        this.serverPort = serverPort;
        this.grpc = new Grpc(serverPort, new IRpcHandler() {
            @Override
            public void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto) {
                System.out.println(requestVoteDto);
            }

            @Override
            public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
                System.out.println(requestVoteResponseDto);
            }

            @Override
            public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
                System.out.println(appendEntriesDto);
            }

            @Override
            public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {
                System.out.println(appendEntriesResponseDto);
            }
        });
    }

    public void Start(){



    }

}

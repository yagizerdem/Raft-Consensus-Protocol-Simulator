package Raft;

import Models.*;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;

import java.util.ArrayList;
import java.util.Random;

public class RaftModule {

    public int serverPort;

    public ArrayList<Integer> peers;
    private Grpc grpc;

    private double electionTimeout;

    private double minTimeout = 2000;

    private double maxTimeout = 5000;

    private long timeFragment = 50;

    private Random random = new Random();

    private Storage storage;

    public  RaftModule(int serverPort, ArrayList<Integer> peers){
        this.serverPort = serverPort;
        this.electionTimeout = minTimeout + (maxTimeout - minTimeout) * random.nextDouble();
        this.peers = peers;
        this.storage = new Storage(serverPort);
    }

    public void Start(){
        try{
            this.storage.initialize();

            manageTimeout();

            this.grpc = new Grpc(serverPort, new IRpcHandler() {
                @Override
                public void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto) {
                    RaftModule.this.handleRequestVoteRpc(requestVoteDto);
                }

                @Override
                public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
                    RaftModule.this.handleRequestVoteResponseRpc(requestVoteResponseDto);
                }

                @Override
                public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
                    RaftModule.this.handleAppendEntriesRpc(appendEntriesDto);
                }

                @Override
                public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {
                    RaftModule.this.handleAppendEntriesResponseRpc(appendEntriesResponseDto);
                }
            });
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }


    public void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto){
        System.out.println(requestVoteDto);
    }

    public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
        System.out.println(requestVoteResponseDto);
    }

    public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
        System.out.println(appendEntriesDto);
    }

    public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {
        System.out.println(appendEntriesResponseDto);
    }

    public void manageTimeout(){
        new Thread(() -> {
            try{
                while (true) {
                    if(electionTimeout < 0) {
                        this.electionTimeout = minTimeout + (maxTimeout - minTimeout) * random.nextDouble();
                        StartElection();
                    }

                    Thread.sleep(this.timeFragment);
                    this.electionTimeout -= this.timeFragment;
                }
            }catch (Exception ex){
                System.out.println(ex.getMessage());
            }

        }).start();
    }

    public void StartElection(){
        RequestVoteRPCDTO dto = new RequestVoteRPCDTO();

        // send other server request vote rpc message
//        for(int i = 0 ; i < this.peers.size(); i++){
//            int peer = this.peers.get(i);
//            grpc.sendRequestVoteRpc(peer, dto);
//        }


        System.out.println(serverPort + " server started election !");
    }

}

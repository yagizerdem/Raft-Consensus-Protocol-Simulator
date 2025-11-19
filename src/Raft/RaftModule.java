package Raft;

import Models.*;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;

public class RaftModule {

    public int serverPort;

    public ArrayList<Integer> peers;

    public int voteRecievedCounter = 0;
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
        synchronized (this.storage.lock){
            if(requestVoteDto.term > this.storage.getCurrentTerm()) {
                this.storage.setServerLevel(ServerLevel.Follower);
                this.storage.setVotedFor(null);
                this.voteRecievedCounter = 0;
                this.storage.setCurrentTerm(requestVoteDto.term);
            }

            RequestVoteResultRPCDTO response = new RequestVoteResultRPCDTO();
            if(requestVoteDto.term < this.storage.getCurrentTerm()) {
                // fail
                response.term = this.storage.getCurrentTerm();
                response.voteGranted = false;
                this.grpc.sendRequestVoteResponseRpc(
                        Integer.parseInt(requestVoteDto.candidateId), response);
                return;
            }

            if (this.storage.getVotedFor() != null &&
                    !this.storage.getVotedFor().equals(requestVoteDto.candidateId)) {
                // fail
                response.term = this.storage.getCurrentTerm();
                response.voteGranted = false;
                this.grpc.sendRequestVoteResponseRpc(
                        Integer.parseInt(requestVoteDto.candidateId), response);
                return;
            }

            if(this.storage.getLogs().size() > 0 &&
                    requestVoteDto.term < this.storage.getLogs().get(this.storage.getLogs().size() -1).term) {
                // fail
                response.term = this.storage.getCurrentTerm();
                response.voteGranted = false;
                this.grpc.sendRequestVoteResponseRpc(
                        Integer.parseInt(requestVoteDto.candidateId), response);
                return;
            }

            if(this.storage.getLogs().size() > 0 &&
                    requestVoteDto.term == this.storage.getLogs().get(this.storage.getLogs().size() -1).term &&
                    requestVoteDto.lastLogIndex < this.storage.getLogs().get(this.storage.getLogs().size() -1).index) {
                // fail
                response.term = this.storage.getCurrentTerm();
                response.voteGranted = false;
                this.grpc.sendRequestVoteResponseRpc(
                        Integer.parseInt(requestVoteDto.candidateId), response);
                return;
            }

            response.term = this.storage.getCurrentTerm();
            response.voteGranted = true;
            this.grpc.sendRequestVoteResponseRpc(
                    Integer.parseInt(requestVoteDto.candidateId), response);
        }

    }

    public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
        synchronized (this.storage.lock) {
            if(requestVoteResponseDto.term > this.storage.getCurrentTerm()) {
                this.storage.setServerLevel(ServerLevel.Follower);
                this.storage.setVotedFor(null);
                this.voteRecievedCounter = 0;
                this.storage.setCurrentTerm(requestVoteResponseDto.term);
                return;
            }

            if(requestVoteResponseDto.voteGranted) {
                this.voteRecievedCounter += 1;
            }

            if(this.voteRecievedCounter >= ((this.peers.size() + 1) / 2) + 1) {
                this.storage.setServerLevel(ServerLevel.Leader);
                this.storage.setVotedFor(null);
                this.voteRecievedCounter = 0;

                System.out.println("leader elected new leader is " + this.serverPort );

            }
        }

    }

    public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
        System.out.println(appendEntriesDto);
    }

    public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {
        System.out.println(appendEntriesResponseDto);
    }

    public void manageTimeout(){
        new Thread(() -> {
            try {
                while (true) {
                    synchronized (this.storage.lock) {
                        if (this.storage.getServerLevel().equals(ServerLevel.Leader)) {
                        } else {
                            if (electionTimeout < 0) {
                                electionTimeout = minTimeout + (maxTimeout - minTimeout) * random.nextDouble();
                                StartElection();
                            }
                            electionTimeout -= timeFragment;
                        }
                    }

                    Thread.sleep(timeFragment);

                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }).start();

    }

    public  void StartElection(){
        RequestVoteRPCDTO dto = new RequestVoteRPCDTO();
        synchronized (this.storage.lock){
            if(this.storage.getServerLevel().equals(ServerLevel.Leader)) return;
            // vote for self
            this.storage.setVotedFor(String.valueOf(this.serverPort));
            this.voteRecievedCounter = 1;
            // increase term
            this.storage.setCurrentTerm(this.storage.getCurrentTerm() + 1);
            // conever to candidate
            this.storage.setServerLevel(ServerLevel.Candidate);


            dto.candidateId = String.valueOf(this.serverPort);
            dto.term = this.storage.getCurrentTerm();
            dto.lastLogIndex = this.storage.getLogs().size() > 0 ?
                    this.storage.getLogs().get(this.storage.getLogs().size() -1).index : -1;
            dto.lastLogTerm = this.storage.getLogs().size() > 0 ?
                    this.storage.getLogs().get(this.storage.getLogs().size() -1).term : -1;

        }
        // send other server request vote rpc message
        for(int i = 0 ; i < this.peers.size(); i++){
            int peer = this.peers.get(i);
            grpc.sendRequestVoteRpc(peer, dto);
        }
        System.out.println("new election started by " + this.serverPort);

    }

}

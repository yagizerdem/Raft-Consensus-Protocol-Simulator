package Raft;

import Models.*;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;

import javax.swing.*;
import javax.swing.plaf.TableHeaderUI;
import java.io.PipedReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;
import java.util.UUID;

public class RaftModule {

    public int serverPort;

    public ArrayList<Integer> peers;

    public float minTime = 2000;

    public float maxTime = 8000;

    public float timeFragment = 500;

    public float timeOut = 0;

    private Storage storage;

    private RedirectOutput redirectOutput;

    private Grpc grpc;

    private int voteCounter = 0;

    public RaftModule(int serverPort, ArrayList<Integer> peers){
        this.serverPort = serverPort;
        this.peers = peers;
        this.timeOut = this.generateRandomTime();
        this.storage = new Storage(serverPort);
        this.redirectOutput = new RedirectOutput(serverPort);

    }

    public void Start(){
        try{
            this.storage.initialize();

            this.manageTimeout();
            this.manageHeartbeat();
            this.grpc = new Grpc(this.serverPort, new IRpcHandler() {
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

                }

                @Override
                public void handleClientCommandRpc(ClientCommandRPCDTO clientCommandDto) {

                }
            });
        }catch (Exception ex){
            redirectOutput.WriteCerr(ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }

    private void handleRequestVoteRpc(RequestVoteRPCDTO req) {

        if (req.term < this.storage.getCurrentTerm()) {
            sendVote(req, false);
            return;
        }

        if (req.term > this.storage.getCurrentTerm()) {
            this.stepDownFollower(req.term);
        }

        Log myLast = this.storage.getLastLog();
        long myTerm = (myLast != null ? myLast.term : 0);
        long myIndex = (myLast != null ? myLast.index : 0);

        boolean candidateUpToDate = false;

        if (req.lastLogTerm > myTerm) {
            candidateUpToDate = true;
        } else if (req.lastLogTerm == myTerm && req.lastLogIndex >= myIndex) {
            candidateUpToDate = true;
        }

        boolean canVote =
                (this.storage.getVotedFor() == null ||
                        this.storage.getVotedFor().equals(req.candidateId));

        if (canVote && candidateUpToDate) {
            this.storage.setVotedFor(req.candidateId);
            sendVote(req, true);
            return;
        }

        sendVote(req, false);
    }

    private void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO response) {
        // Step down if term is higher
        if (response.term > this.storage.getCurrentTerm()) {
            this.stepDownFollower(response.term);
            return;
        }

        // Count vote
        if (response.voteGranted) {
            this.voteCounter++;
        }

        int clusterSize = this.peers.size() + 1;
        int majority = (clusterSize / 2) + 1;

        // Become leader
        if (this.voteCounter >= majority) {

            this.storage.setServerLevel(ServerLevel.Leader);
            this.voteCounter = 0; // reset

            // Initialize leader state
            Log last = this.storage.getLastLog();
            long lastIndex = (last != null ? last.index : 0);

            Hashtable<Integer, Integer> next = new Hashtable<Integer, Integer>();
            Hashtable<Integer, Integer> match = new Hashtable<Integer, Integer>();

            for (int peer : peers) {
                next.put(peer, (int)lastIndex + 1);
                match.put(peer, 0);
            }

            synchronized (this.storage.lock) {
                this.storage.setNextIndex(next);
                this.storage.setMatchIndex(match);
            }


        }
    }

    private void handleAppendEntriesRpc(AppendEntriesRPCDTO req) {
        // Step down if term is higher
        if (req.term > this.storage.getCurrentTerm()) {
            this.stepDownFollower(req.term);
        }

        // heartbeat recieved
        if(req.entries.isEmpty()) {
            this.timeOut = generateRandomTime();
        }
    }

    public void manageTimeout() {
        new Thread(() ->{
        while (true){
            try{
                if(this.timeOut <= 0) {
                    startElection();
                }
                this.timeOut = Math.max(this.timeOut - this.timeFragment, 0);
                Thread.sleep((int)this.timeFragment);
            }catch (Exception ex) {
                System.out.println(ex.getMessage());
            };
        }


        }).start();
    }

    public void startElection(){
        if(this.storage.getServerLevel().equals(ServerLevel.Leader)) return; // leader cannot start election

        synchronized (this.storage.lock){
            this.storage.setCurrentTerm(this.storage.getCurrentTerm() + 1);
            // vote for self
            this.storage.setVotedFor(String.valueOf(this.serverPort));
            this.voteCounter = 1;
            this.storage.setServerLevel(ServerLevel.Candidate); // step into candidate
        }

        for(Integer peer : this.peers){
            RequestVoteRPCDTO dto = new RequestVoteRPCDTO();
            dto.traceId = generateUUid();
            dto.term = this.storage.getCurrentTerm();
            Log lastLog = this.storage.getLastLog();
            dto.lastLogIndex = lastLog != null ? lastLog.index : 0;
            dto.lastLogTerm = lastLog != null ? lastLog.term : 0;
            dto.candidateId = String.valueOf(this.serverPort);

            grpc.sendRequestVoteRpc(peer, dto);
        }

        this.timeOut = generateRandomTime();
    }

    public void manageHeartbeat(){
        new Thread(() ->{
            while (true) {
                try{
                    System.out.println(this.serverPort + " " +  this.storage.getServerLevel());
                    // only leader can send heartbeat
                    if(this.storage.getServerLevel().equals(ServerLevel.Leader)) {
                        for(int peer : this.peers){
                            AppendEntriesRPCDTO dto = new AppendEntriesRPCDTO();
                            dto.term = this.storage.getCurrentTerm();
                            dto.entries = new ArrayList<>();
                            dto.traceId = generateUUid();

                            this.grpc.sendAppendEntriesRpc(peer, dto);
                        }
                    }

                    Thread.sleep((int)this.timeFragment);
                }catch (Exception ex){
                    System.out.println(ex.getMessage());
                }
            }
        }).start();
    }

    // auxilary
    public float generateRandomTime() {
        return minTime + (float)(Math.random() * (maxTime - minTime));
    }
    public String generateUUid(){
        return UUID.randomUUID().toString();
    }
    public void stepDownFollower(long newTerm){
        synchronized (this.storage.lock) {
            this.storage.setServerLevel(ServerLevel.Follower);
            this.timeOut = generateRandomTime();
            this.voteCounter = 0;
            this.storage.setVotedFor(null);
            this.storage.setCurrentTerm(newTerm);
        }
    }



    private void sendVote(RequestVoteRPCDTO req, boolean granted) {
        RequestVoteResultRPCDTO res = new RequestVoteResultRPCDTO();
        res.traceId = req.traceId;
        res.term = this.storage.getCurrentTerm();
        res.voteGranted = granted;

        this.grpc.sendRequestVoteResponseRpc(
                Integer.parseInt(req.candidateId), res);
    }
}

package Raft;

import Models.*;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;
import com.sun.jdi.event.ThreadStartEvent;

import javax.swing.*;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.text.Style;
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

    public Hashtable<String, AppendEntriesRPCDTO> appendEntriesRpcDtoCache = new Hashtable<>();

    public Hashtable<String, Integer> appendEntryFollowerSocketPort = new Hashtable<>();

    public boolean initAsLeader;
    public RaftModule(int serverPort, ArrayList<Integer> peers, boolean initAsLeader){
        this.serverPort = serverPort;
        this.peers = peers;
        this.timeOut = this.generateRandomTime();
        this.storage = new Storage(serverPort);
        this.redirectOutput = new RedirectOutput(serverPort);
        this.initAsLeader = initAsLeader;

    }

    public void Start(){
        try{
            this.storage.initialize();

            this.manageTimeout();
            this.manageHeartbeat();
            this.manageAppendEntries();

            if(this.initAsLeader) {
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

                System.out.println(this.serverPort + " " +  this.storage.getServerLevel());
            }
            else{
                this.storage.setServerLevel(ServerLevel.Follower);
            }


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
                    RaftModule.this.handleAppendResponseRpc(appendEntriesResponseDto);
                }

                @Override
                public void handleClientCommandRpc(ClientCommandRPCDTO clientCommandDto) {
                    RaftModule.this.handleClientCommandRpc(clientCommandDto);
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

            System.out.println(this.serverPort + " " +  this.storage.getServerLevel());

        }
    }

    private void handleAppendEntriesRpc(AppendEntriesRPCDTO req) {
        // Step down if term is higher
        if (req.term > this.storage.getCurrentTerm()) {
            this.stepDownFollower(req.term);
        }

        // heartbeat received
        if(req.entries.isEmpty()) {
            this.timeOut = generateRandomTime();
        }

        AppendEntriesRPCResultDTO result = new AppendEntriesRPCResultDTO();
        result.term = this.storage.getCurrentTerm();
        result.traceId = req.traceId;

        // 1. Reply false if term < currentTerm
        if(req.term < this.storage.getCurrentTerm()) {
            result.success = false;
            this.grpc.sendAppendEntriesResponseRpc(Integer.valueOf(req.leaderId), result);
            return;
        }

        // 2. Reply false if log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm
        if(req.prevLogIndex > 0) {
            Log prevLog = this.storage.getLogByIndex((int)req.prevLogIndex);
            if(prevLog == null) {
                result.success = false;
                this.grpc.sendAppendEntriesResponseRpc(Integer.valueOf(req.leaderId), result);
                return;
            }
            if(prevLog.term != req.prevLogTerm) {
                result.success = false;
                this.grpc.sendAppendEntriesResponseRpc(Integer.valueOf(req.leaderId), result);
                return;
            }
        }


        //3. If an existing entry conflicts with a new one(same index but different terms),delete the existing entry and all that follow it
        for(int j = 0; j < req.entries.size(); j++){
            Log entry = req.entries.get(j);
            Log existing = this.storage.getLogByIndex((int)entry.index);
            if(existing !=  null && existing.term != entry.term){
                // should delete from logs
                this.storage.deleteFromIndex((int)entry.index);
                break;
            }
        }

        //4. Append any new entries not already in the log
        for(int j = 0; j < req.entries.size(); j++){
            Log entry = req.entries.get(j);
            this.storage.appendLogEntry(entry);
        }

        // 5. If leader Commit > commitIndex , set commitIndex= min(leaderCommit, index of last new entry)
        if(req.leaderCommit > this.storage.getCommitIndex()) {
            int lastNewIndex;

            if(req.entries.isEmpty()) {
                Log lastLocal = this.storage.getLastLog();
                lastNewIndex = (int)(lastLocal == null ? 0 : lastLocal.index);
            } else {
                Log lastNew = req.entries.get(req.entries.size() - 1);
                lastNewIndex = (int)lastNew.index;
            }
            if(req.leaderCommit > this.storage.getCommitIndex()) {
                int newCommitIdx = (int)Math.min(req.leaderCommit, lastNewIndex);
                this.storage.setCommitIndex(newCommitIdx);
            }
        }

        result.success = true;
        this.grpc.sendAppendEntriesResponseRpc(Integer.valueOf(req.leaderId), result);
    }

    private void handleAppendResponseRpc(AppendEntriesRPCResultDTO response) {

        synchronized (this.storage.lock) {
            int follower = appendEntryFollowerSocketPort.get(response.traceId);

            if (response.term > storage.getCurrentTerm()) {
                stepDownFollower(response.term);
                return;
            }

            if (!response.success) {
                int old = storage.getNextIndex().get(follower);
                storage.getNextIndex().put(follower, Math.max(old - 1, 1));
                return;
            }

            AppendEntriesRPCDTO req = appendEntriesRpcDtoCache.get(response.traceId);

            if (req.entries.isEmpty()) {
                return;
            }

            int lastSentIndex = (int)(req.prevLogIndex + req.entries.size());

            storage.getMatchIndex().put(follower, lastSentIndex);
            storage.getNextIndex().put(follower, lastSentIndex + 1);

            tryCommitEntries();
        }
    }

    private void handleClientCommandRpc(ClientCommandRPCDTO dto) {
        if(this.storage.getServerLevel().equals(ServerLevel.Leader)) {
            Log entry = new Log();
            entry.term = this.storage.getCurrentTerm();
            entry.index = this.storage.getLastLog() == null ? 1 :
                    this.storage.getLastLog().index + 1;
            entry.shellCommand =  dto.shellCommand;
            this.storage.appendLogEntry(entry);
        }
    }

    private void tryCommitEntries() {

        int N = storage.getLastLog() != null ? (int) storage.getLastLog().index : 0;
        int majority = (peers.size() + 1) / 2 + 1;

        for (int index = N; index > storage.getCommitIndex(); index--) {

            if (storage.getLogByIndex(index).term != storage.getCurrentTerm()) {
                continue;
            }

            int count = 1; // leader

            for (int peer : peers) {
                if (storage.getMatchIndex().get(peer) >= index)
                    count++;
            }

            if (count >= majority) {
                storage.setCommitIndex(index);
                return;
            }
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
                    // only leader can send heartbeat
                    if(this.storage.getServerLevel().equals(ServerLevel.Leader)) {
                        for(int i = 0; i < peers.size(); i++){
                            int peer = peers.get(i);
                            Integer nextLogIndex = this.storage.getNextIndex().get(peer);

                            AppendEntriesRPCDTO dto = new AppendEntriesRPCDTO();
                            dto.term = this.storage.getCurrentTerm();
                            dto.traceId = generateUUid();
                            dto.leaderId = String.valueOf(this.serverPort);
                            dto.leaderCommit = this.storage.getCommitIndex();
                            dto.prevLogIndex = nextLogIndex -1 ;

                            Log prevLog = this.storage.getLogByIndex(nextLogIndex - 1);
                            dto.prevLogTerm = prevLog == null ? 0 : prevLog.term;
                            // bullet proof entries selection code -> gpt generated
                            int lastIndex = this.storage.getLogs().size();

                            dto.entries = new ArrayList<>();

                            this.appendEntriesRpcDtoCache.put(dto.traceId, dto);
                            this.appendEntryFollowerSocketPort.put(dto.traceId, peer);
                            this.grpc.sendAppendEntriesRpc(peer,  dto);
                        }
                    }

                    Thread.sleep((int)this.timeFragment);
                }catch (Exception ex){
                    System.out.println(ex.getMessage());
                }
            }
        }).start();
    }

    public void manageAppendEntries(){
        new Thread(() ->{
            while (true){
                try{

                    if(!this.storage.getServerLevel().equals(ServerLevel.Leader)) {
                        Thread.sleep((int)this.timeFragment);
                        continue;
                    }
                    System.out.println("hit 2");

                    for(int i = 0; i < peers.size(); i++){
                        int peer = peers.get(i);
                        Integer nextLogIndex = this.storage.getNextIndex().get(peer);

                        AppendEntriesRPCDTO dto = new AppendEntriesRPCDTO();
                        dto.term = this.storage.getCurrentTerm();
                        dto.traceId = generateUUid();
                        dto.leaderId = String.valueOf(this.serverPort);
                        dto.leaderCommit = this.storage.getCommitIndex();
                        dto.prevLogIndex = nextLogIndex -1 ;

                        Log prevLog = this.storage.getLogByIndex(nextLogIndex - 1);
                        dto.prevLogTerm = prevLog == null ? 0 : prevLog.term;
                        // bullet proof entries selection code -> gpt generated
                        int lastIndex = this.storage.getLogs().size();

                        if (nextLogIndex <= lastIndex) {
                            dto.entries = new ArrayList<>(
                                    this.storage.getLogs().subList(nextLogIndex -1, lastIndex)
                            );
                        } else {
                            dto.entries = new ArrayList<>();
                        }

                        this.appendEntriesRpcDtoCache.put(dto.traceId, dto);
                        this.appendEntryFollowerSocketPort.put(dto.traceId, peer);
                        this.grpc.sendAppendEntriesRpc(peer,  dto);

                    }

                    Thread.sleep((int)this.timeFragment);
                }catch (Exception ex) {
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

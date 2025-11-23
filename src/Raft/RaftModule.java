package Raft;

import Models.*;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;
import com.sun.jdi.event.ThreadStartEvent;

import javax.sound.midi.Soundbank;
import javax.swing.*;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.text.Style;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    public int stateMachineLogIndex = 1;

    private int clientPort;

    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    public RaftModule(int serverPort, ArrayList<Integer> peers, boolean initAsLeader, int clientPort){
        this.serverPort = serverPort;
        this.peers = peers;
        this.timeOut = this.generateRandomTime();
        this.storage = new Storage(serverPort);
        this.redirectOutput = new RedirectOutput(serverPort);
        this.initAsLeader = initAsLeader;
        this.stateMachineLogIndex = 1;
        this.clientPort = clientPort;
    }

    public void Start(){
        try{
            this.storage.initialize();
            this.redirectOutput.initialize();

            this.manageTimeout();
            this.manageHeartbeat();
            this.manageAppendEntries();
            this.manageStateMachine();

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

                System.out.println("Server started at localhost port : " + this.serverPort);
                System.out.println("Initialized server authority level as : " + this.storage.getServerLevel());
                this.redirectOutput.WriteCerr("Server started at localhost port : " + this.serverPort);
                this.redirectOutput.WriteCout("Initialized server authority level as : " + this.storage.getServerLevel());
            }
            else{
                this.storage.setServerLevel(ServerLevel.Follower);
                this.redirectOutput.WriteCout("Initialized server authority level as : " + ServerLevel.Follower);
                System.out.println("Initialized server authority level as : " + ServerLevel.Follower);
            }


            this.grpc = new Grpc(this.serverPort, new IRpcHandler() {
                @Override
                public void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto) {
                    RaftModule.this.redirectOutput.WriteCout(
                            "[RequestVote RPC RECEIVED] " +
                                    "from candidate=" + requestVoteDto.candidateId +
                                    " term=" + requestVoteDto.term +
                                    " lastLogIndex=" + requestVoteDto.lastLogIndex +
                                    " lastLogTerm=" + requestVoteDto.lastLogTerm
                    );
                    RaftModule.this.handleRequestVoteRpc(requestVoteDto);
                }

                @Override
                public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
                    RaftModule.this.redirectOutput.WriteCout(
                            "[RequestVote RESPONSE RECEIVED] " +
                                    "traceId=" + requestVoteResponseDto.traceId +
                                    " term=" + requestVoteResponseDto.term +
                                    " voteGranted=" + requestVoteResponseDto.voteGranted
                    );
                    RaftModule.this.handleRequestVoteResponseRpc(requestVoteResponseDto);
                }

                @Override
                public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
                    RaftModule.this.redirectOutput.WriteCout(
                            "[AppendEntries RPC RECEIVED] " +
                                    "from leader=" + appendEntriesDto.leaderId +
                                    " traceId=" + appendEntriesDto.traceId +
                                    " term=" + appendEntriesDto.term +
                                    " prevLogIndex=" + appendEntriesDto.prevLogIndex +
                                    " prevLogTerm=" + appendEntriesDto.prevLogTerm +
                                    " entries=" + appendEntriesDto.entries.size() +
                                    " leaderCommit=" + appendEntriesDto.leaderCommit
                    );
                    RaftModule.this.handleAppendEntriesRpc(appendEntriesDto);
                }

                @Override
                public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {
                    RaftModule.this.redirectOutput.WriteCout(
                            "[AppendEntries RESPONSE RECEIVED] " +
                                    "traceId=" + appendEntriesResponseDto.traceId +
                                    " term=" + appendEntriesResponseDto.term +
                                    " success=" + appendEntriesResponseDto.success
                    );
                    RaftModule.this.handleAppendResponseRpc(appendEntriesResponseDto);
                }

                @Override
                public void handleClientCommandRpc(ClientCommandRPCDTO clientCommandDto) {
                    RaftModule.this.redirectOutput.WriteCout(
                            "[CLIENT COMMAND RECEIVED] " +
                                    "clientPort=" + clientCommandDto.clientPort +
                                    " shellCommand=\"" + clientCommandDto.shellCommand + "\""
                    );

                    RaftModule.this.handleClientCommandRpc(clientCommandDto);
                }

                @Override
                public void handleClientCommandResponseRpc(ClientCommandRPCResultDTO clientCommandRPCResultDTO) {
                    throw new UnsupportedOperationException(); // server should not handle client response
                }


            });
        }catch (Exception ex){
            redirectOutput.WriteCerr(ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }

    private void handleRequestVoteRpc(RequestVoteRPCDTO req) {

        Log myLast = this.storage.getLastLog();
        long myTerm = (myLast != null ? myLast.term : 0);
        long myIndex = (myLast != null ? myLast.index : 0);
        boolean candidateUpToDate = false;

        this.redirectOutput.WriteCout(
                "[RequestVote RPC] RECEIVED\n" +
                        "  from candidate=" + req.candidateId + "\n" +
                        "  traceId=" + req.traceId + "\n" +
                        "  candidateTerm=" + req.term + "\n" +
                        "  candidateLastLogIndex=" + req.lastLogIndex + "\n" +
                        "  candidateLastLogTerm=" + req.lastLogTerm + "\n" +
                        "  myTerm=" + this.storage.getCurrentTerm() + "\n" +
                        "  myLastLogIndex=" + myIndex + "\n" +
                        "  myLastLogTerm=" + myTerm
        );


        if (req.term < this.storage.getCurrentTerm()) {
            this.redirectOutput.WriteCout(
                    "[RequestVote RPC] DENIED -> Candidate term lower than current term"
            );
            sendVote(req, false);
            return;
        }

        if (req.term > this.storage.getCurrentTerm()) {
            this.redirectOutput.WriteCout(
                    "[RequestVote RPC] Newer term detected -> Stepping down to FOLLOWER"
            );
            this.stepDownFollower(req.term);
        }

        if (req.lastLogTerm > myTerm) {
            candidateUpToDate = true;
        } else if (req.lastLogTerm == myTerm && req.lastLogIndex >= myIndex) {
            candidateUpToDate = true;
        }

        this.redirectOutput.WriteCout(
                "[RequestVote RPC] Log comparison result -> candidateUpToDate=" + candidateUpToDate
        );

        boolean canVote =
                (this.storage.getVotedFor() == null ||
                        this.storage.getVotedFor().equals(req.candidateId));

        this.redirectOutput.WriteCout(
                "[RequestVote RPC] Already votedFor=" + this.storage.getVotedFor() +
                        " -> canVote=" + canVote
        );

        if (canVote && candidateUpToDate) {
            this.storage.setVotedFor(req.candidateId);
            this.redirectOutput.WriteCout(
                    "[RequestVote RPC] VOTE GRANTED -> candidate=" + req.candidateId
            );
            sendVote(req, true);
            return;
        }


        this.redirectOutput.WriteCout(
                "[RequestVote RPC] VOTE DENIED -> Conditions not met"
        );

        sendVote(req, false);
    }

    private void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO response) {

        this.redirectOutput.WriteCout(
                "[RequestVote RESPONSE] RECEIVED\n" +
                        "  traceId=" + response.traceId + "\n" +
                        "  from term=" + response.term + "\n" +
                        "  voteGranted=" + response.voteGranted + "\n" +
                        "  currentTerm=" + this.storage.getCurrentTerm()
        );

        // Step down if term is higher
        if (response.term > this.storage.getCurrentTerm()) {
            this.redirectOutput.WriteCout(
                    "[RequestVote RESPONSE] HIGHER TERM DETECTED -> stepping down to FOLLOWER"
            );
            this.stepDownFollower(response.term);
            return;
        }

        // Count vote
        if (response.voteGranted) {
            this.voteCounter++;
            this.redirectOutput.WriteCout(
                    "[RequestVote RESPONSE] Vote granted -> voteCounter=" + this.voteCounter
            );
        }else {
            this.redirectOutput.WriteCout(
                    "[RequestVote RESPONSE] Vote denied by peer"
            );
        }

        int clusterSize = this.peers.size() + 1;
        int majority = (clusterSize / 2) + 1;

        this.redirectOutput.WriteCout(
                "[RequestVote RESPONSE] Majority check -> " +
                        "voteCounter=" + this.voteCounter +
                        ", majority=" + majority
        );

        // Become leader
        if (this.voteCounter >= majority) {

            this.storage.setServerLevel(ServerLevel.Leader);
            this.voteCounter = 0; // reset

            this.redirectOutput.WriteCout(
                    "[ELECTION RESULT] I AM THE NEW LEADER -> port=" + this.serverPort
            );

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


            this.redirectOutput.WriteCout(
                    "[LEADER INIT] nextIndex & matchIndex initialized for all followers"
            );

            System.out.println(this.serverPort + " " +  this.storage.getServerLevel());

        }
    }

    private void handleAppendEntriesRpc(AppendEntriesRPCDTO req) {

        this.redirectOutput.WriteCout(
                "[AppendEntries RPC] RECEIVED\n" +
                        "  from leader=" + req.leaderId + "\n" +
                        "  traceId=" + req.traceId + "\n" +
                        "  term=" + req.term + "\n" +
                        "  prevLogIndex=" + req.prevLogIndex + "\n" +
                        "  prevLogTerm=" + req.prevLogTerm + "\n" +
                        "  entriesCount=" + req.entries.size() + "\n" +
                        "  leaderCommit=" + req.leaderCommit + "\n" +
                        "  myTerm=" + this.storage.getCurrentTerm() + "\n" +
                        "  myCommitIndex=" + this.storage.getCommitIndex()
        );



        // Step down if term is higher
        if (req.term > this.storage.getCurrentTerm()) {
            this.redirectOutput.WriteCout(
                    "[AppendEntries RPC] Newer term detected -> stepping down to FOLLOWER"
            );
            this.stepDownFollower(req.term);
        }

        // heartbeat received
        if(req.entries.isEmpty()) {
            this.redirectOutput.WriteCout(
                    "[AppendEntries RPC] Heartbeat received -> resetting timeout"
            );
            this.timeOut = generateRandomTime();
        }

        AppendEntriesRPCResultDTO result = new AppendEntriesRPCResultDTO();
        result.term = this.storage.getCurrentTerm();
        result.traceId = req.traceId;

        // 1. Reply false if term < currentTerm
        if(req.term < this.storage.getCurrentTerm()) {
            this.redirectOutput.WriteCout(
                    "[AppendEntries RPC] REJECTED -> leader term is stale"
            );
            result.success = false;
            this.grpc.sendAppendEntriesResponseRpc(Integer.valueOf(req.leaderId), result);
            return;
        }

        // 2. Reply false if log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm
        if(req.prevLogIndex > 0) {
            Log prevLog = this.storage.getLogByIndex((int)req.prevLogIndex);
            if(prevLog == null) {
                this.redirectOutput.WriteCout(
                        "[AppendEntries RPC] REJECTED -> Missing prevLogIndex=" + req.prevLogIndex
                );
                result.success = false;
                this.grpc.sendAppendEntriesResponseRpc(Integer.valueOf(req.leaderId), result);
                return;
            }
            if(prevLog.term != req.prevLogTerm) {
                this.redirectOutput.WriteCout(
                        "[AppendEntries RPC] REJECTED -> prevLogTerm mismatch. " +
                                "Expected=" + prevLog.term + ", got=" + req.prevLogTerm
                );
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
                this.redirectOutput.WriteCout(
                        "[AppendEntries RPC] CONFLICT DETECTED at index=" + entry.index +
                                " (existingTerm=" + existing.term + ", newTerm=" + entry.term + ")\n" +
                                "  -> Deleting all logs starting from this index"
                );
                // should delete from logs
                this.storage.deleteFromIndex((int)entry.index);
                break;
            }
        }

        //4. Append any new entries not already in the log
        for(int j = 0; j < req.entries.size(); j++){
            Log entry = req.entries.get(j);
            this.redirectOutput.WriteCout(
                    "[AppendEntries RPC] Appending entry -> index=" + entry.index +
                            ", term=" + entry.term
            );
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
                this.redirectOutput.WriteCout(
                        "[AppendEntries RPC] Updating commitIndex -> " +
                                this.storage.getCommitIndex() + " -> " + newCommitIdx
                );
                this.storage.setCommitIndex(newCommitIdx);
            }
        }
        this.redirectOutput.WriteCout(
                "[AppendEntries RPC] SUCCESS -> Sending response (traceId=" + req.traceId + ")"
        );
        result.success = true;
        this.grpc.sendAppendEntriesResponseRpc(Integer.valueOf(req.leaderId), result);
    }

    private void handleAppendResponseRpc(AppendEntriesRPCResultDTO response) {

        synchronized (this.storage.lock) {
            try{
                int follower = appendEntryFollowerSocketPort.get(response.traceId);

                this.redirectOutput.WriteCout(
                        "[AppendEntries RESPONSE] RECEIVED\n" +
                                "  from follower=" + follower + "\n" +
                                "  traceId=" + response.traceId + "\n" +
                                "  term=" + response.term + "\n" +
                                "  success=" + response.success + "\n" +
                                "  currentTerm=" + this.storage.getCurrentTerm()
                );


                if (response.term > storage.getCurrentTerm()) {
                    this.redirectOutput.WriteCout(
                            "[AppendEntries RESPONSE] Higher term detected -> stepping down to FOLLOWER"
                    );
                    stepDownFollower(response.term);
                    return;
                }

                if (!response.success) {
                    int oldNext = storage.getNextIndex().get(follower);
                    int newNext = Math.max(oldNext - 1, 1);

                    this.redirectOutput.WriteCout(
                            "[AppendEntries RESPONSE] FAIL -> Decreasing nextIndex for follower=" + follower +
                                    " (" + oldNext + " -> " + newNext + ")"
                    );

                    storage.getNextIndex().put(follower, newNext);
                    return;
                }

                AppendEntriesRPCDTO req = appendEntriesRpcDtoCache.get(response.traceId);

                if (req.entries.isEmpty()) {
                    this.redirectOutput.WriteCout(
                            "[AppendEntries RESPONSE] Heartbeat ACK from follower=" + follower
                    );
                    return;
                }

                int lastSentIndex = (int)(req.prevLogIndex + req.entries.size());


                this.redirectOutput.WriteCout(
                        "[AppendEntries RESPONSE] SUCCESS -> Updating matchIndex & nextIndex\n" +
                                "  follower=" + follower + "\n" +
                                "  lastSentIndex=" + lastSentIndex + "\n" +
                                "  nextIndex will be " + (lastSentIndex + 1)
                );

                storage.getMatchIndex().put(follower, lastSentIndex);
                storage.getNextIndex().put(follower, lastSentIndex + 1);

                this.redirectOutput.WriteCout(
                        "[AppendEntries RESPONSE] Trying to advance commit index..."
                );


                tryCommitEntries();
            }finally {
                appendEntriesRpcDtoCache.remove(response.traceId);
                appendEntryFollowerSocketPort.remove(response.traceId);
            }
        }
    }

    private void handleClientCommandRpc(ClientCommandRPCDTO dto) {
        this.redirectOutput.WriteCout(
                "[ClientCommand RPC] RECEIVED\n" +
                        "  clientPort=" + dto.clientPort + "\n" +
                        "  shellCommand=\"" + dto.shellCommand + "\"\n" +
                        "  serverRole=" + this.storage.getServerLevel()
        );

        if(this.storage.getServerLevel().equals(ServerLevel.Leader)) {
            Log entry = new Log();
            entry.term = this.storage.getCurrentTerm();
            entry.index = this.storage.getLastLog() == null ? 1 :
                    this.storage.getLastLog().index + 1;
            entry.shellCommand =  dto.shellCommand;
            this.storage.appendLogEntry(entry);

            this.redirectOutput.WriteCout(
                    "[ClientCommand RPC] ACCEPTED & LOGGED\n" +
                            "  logIndex=" + entry.index + "\n" +
                            "  term=" + entry.term
            );
        }
        else{
            this.redirectOutput.WriteCout(
                    "[ClientCommand RPC] IGNORED -> this server is not LEADER"
            );
        }
    }


    private void tryCommitEntries() {

        int N = storage.getLastLog() != null ? (int) storage.getLastLog().index : 0;
        int majority = (peers.size() + 1) / 2 + 1;

        this.redirectOutput.WriteCout(
                "[CommitCheck] Starting commit attempt...\n" +
                        "  lastLogIndex=" + N + "\n" +
                        "  currentCommitIndex=" + storage.getCommitIndex() + "\n" +
                        "  majority=" + majority + "\n"
        );

        for (int index = N; index > storage.getCommitIndex(); index--) {
            Log logEntry = storage.getLogByIndex(index);
            if (storage.getLogByIndex(index).term != storage.getCurrentTerm()) {
                this.redirectOutput.WriteCout(
                        "[CommitCheck] Skipping index=" + index +
                                " (term mismatch: logTerm=" + logEntry.term +
                                ", currentTerm=" + storage.getCurrentTerm() + ")"
                );
                continue;
            }

            int count = 1; // leader

            for (int peer : peers) {
                if (storage.getMatchIndex().get(peer) >= index)
                    count++;
            }

            this.redirectOutput.WriteCout(
                    "[CommitCheck] index=" + index +
                            " replicatedCount=" + count +
                            "/" + majority
            );

            if (count >= majority) {
                int oldCommit = storage.getCommitIndex();
                storage.setCommitIndex(index);

                this.redirectOutput.WriteCout(
                        "[CommitCheck] COMMIT SUCCESS -> commitIndex " +
                                oldCommit + " -> " + index
                );

                return;
            }
        }
    }

    public void manageTimeout() {
        new Thread(() ->{
        while (true){
            try{
                if(this.timeOut <= 0) {
                    this.redirectOutput.WriteCout(
                            "[Timeout] Election timeout reached -> starting election\n" +
                                    "  currentTerm=" + this.storage.getCurrentTerm() + "\n" +
                                    "  serverState=" + this.storage.getServerLevel()
                    );

                    startElection();
                }
                float oldTime = this.timeOut;
                this.timeOut = Math.max(this.timeOut - this.timeFragment, 0);
                this.redirectOutput.WriteCout(
                        "[Timeout] ticking -> " + oldTime + " -> " + this.timeOut
                );


                Thread.sleep((int)this.timeFragment);
            }catch (Exception ex) {
                System.out.println(ex.getMessage());
                this.redirectOutput.WriteCerr(
                        "[Timeout ERROR] " + ex.getMessage()
                );
            };
        }


        }).start();
    }

    public void startElection(){
        if(this.storage.getServerLevel().equals(ServerLevel.Leader)) {
            this.redirectOutput.WriteCout("[Election] IGNORE -> Leader cannot start election.");
            return;
        } // leader cannot start election

        synchronized (this.storage.lock){
            long oldTerm = this.storage.getCurrentTerm();
            long newTerm = oldTerm + 1;

            this.storage.setCurrentTerm(newTerm);
            // vote for self
            this.storage.setVotedFor(String.valueOf(this.serverPort));
            this.voteCounter = 1;
            this.storage.setServerLevel(ServerLevel.Candidate); // step into candidate

            this.redirectOutput.WriteCout(
                    "[Election] Starting election...\n" +
                            "  term=" + newTerm + "\n" +
                            "  votedFor=self (" + this.serverPort + ")\n" +
                            "  initialVoteCount=1"
            );
        }

        for(Integer peer : this.peers){
            RequestVoteRPCDTO dto = new RequestVoteRPCDTO();
            dto.traceId = generateUUid();
            dto.term = this.storage.getCurrentTerm();
            Log lastLog = this.storage.getLastLog();
            dto.lastLogIndex = lastLog != null ? lastLog.index : 0;
            dto.lastLogTerm = lastLog != null ? lastLog.term : 0;
            dto.candidateId = String.valueOf(this.serverPort);

            this.redirectOutput.WriteCout(
                    "[Election] Sending RequestVote -> peer=" + peer + "\n" +
                            "  term=" + dto.term + "\n" +
                            "  lastLogIndex=" + dto.lastLogIndex + "\n" +
                            "  lastLogTerm=" + dto.lastLogTerm + "\n" +
                            "  traceId=" + dto.traceId
            );

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

                        this.redirectOutput.WriteCout("[Heartbeat] Leader sending heartbeat...");

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

                            this.redirectOutput.WriteCout(
                                    "[Heartbeat] → peer=" + peer + "\n" +
                                            "  traceId=" + dto.traceId + "\n" +
                                            "  term=" + dto.term + "\n" +
                                            "  prevLogIndex=" + dto.prevLogIndex + "\n" +
                                            "  prevLogTerm=" + dto.prevLogTerm + "\n" +
                                            "  leaderCommit=" + dto.leaderCommit
                            );

                            this.appendEntriesRpcDtoCache.put(dto.traceId, dto);
                            this.appendEntryFollowerSocketPort.put(dto.traceId, peer);
                            this.grpc.sendAppendEntriesRpc(peer,  dto);
                        }
                    }

                    Thread.sleep((int)this.timeFragment);
                }catch (Exception ex){
                    System.out.println(ex.getMessage());
                    this.redirectOutput.WriteCerr("[Heartbeat ERROR] " + ex.getMessage());
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

                            ArrayList<Log> normalizedLogs = new ArrayList<>();
                            for(Log log : dto.entries){
                                String shellCommand = log.shellCommand.trim();
                                String normalizedShellCommand = "";
                                for(int j = 0 ;j <shellCommand.length(); j++){
                                    Character ch = shellCommand.charAt(j);
                                    if(ch.equals('\"')) {
                                        normalizedShellCommand += "\\\"";
                                    }
                                    else{
                                        normalizedShellCommand += ch.toString();
                                    }
                                }
                                normalizedShellCommand = normalizedShellCommand.trim();
                                Log normalizedLog = new Log();
                                normalizedLog.shellCommand = normalizedShellCommand;
                                normalizedLog.term = log.term;
                                normalizedLog.index = log.index;
                                normalizedLogs.add(normalizedLog);
                            }
                            dto.entries = normalizedLogs;

                        } else {
                            dto.entries = new ArrayList<>();
                        }

                        this.redirectOutput.WriteCout(
                                "[AppendEntries] Sending log entries → peer=" + peer + "\n" +
                                        "  traceId=" + dto.traceId + "\n" +
                                        "  term=" + dto.term + "\n" +
                                        "  prevLogIndex=" + dto.prevLogIndex + "\n" +
                                        "  prevLogTerm=" + dto.prevLogTerm + "\n" +
                                        "  entries=" + dto.entries.size() + "\n" +
                                        "  leaderCommit=" + dto.leaderCommit
                        );

                        this.appendEntriesRpcDtoCache.put(dto.traceId, dto);
                        this.appendEntryFollowerSocketPort.put(dto.traceId, peer);
                        this.grpc.sendAppendEntriesRpc(peer,  dto);

                    }

                    Thread.sleep((int)this.timeFragment);
                }catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    this.redirectOutput.WriteCerr("[AppendEntries ERROR] " + ex.getMessage());
                }
            }
        }).start();
    }

    public void manageStateMachine(){
        new Thread(() ->{
            while (true) {
                try{
                    Log currentLog  = this.storage.getLogByIndex(this.stateMachineLogIndex);
                    if(currentLog == null) {
                        Thread.sleep((int)this.timeFragment * 3);
                        continue;
                    };
                    if(this.storage.getCommitIndex() < this.storage.getLastApplied()) {
                        Thread.sleep((int)this.timeFragment * 3);
                        continue;
                    };

                    String shellCommand = currentLog.shellCommand;

                    this.redirectOutput.WriteCout(
                            "[StateMachine] APPLYING LOG ENTRY\n" +
                                    "  index=" + this.stateMachineLogIndex + "\n" +
                                    "  term=" + currentLog.term + "\n" +
                                    "  command=\"" + shellCommand + "\""
                    );

                    String cwd = "./" + serverPort;


                    ProcessBuilder pb = new ProcessBuilder();

                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        pb.command("cmd.exe", "/c", shellCommand);
                    } else {
                        pb.command("sh", "-c", shellCommand);
                    }
                    pb.directory(new File(cwd));

                    pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
                    pb.redirectError(ProcessBuilder.Redirect.PIPE);

                    Process process = pb.start();

                    Future<String> outFuture = exec.submit(() -> readStream(process.getInputStream()));
                    Future<String> errFuture = exec.submit(() -> readStream(process.getErrorStream()));

                    int exitCode = process.waitFor();
                    System.out.println("Shell command exited with code: " + exitCode);

                    String output = outFuture.get();
                    String error  = errFuture.get();

                    this.redirectOutput.WriteCout(
                            "[StateMachine] COMMAND EXECUTED -> exitCode=" + exitCode
                    );

                    this.stateMachineLogIndex += 1;
                    this.storage.setLastApplied(this.storage.getLastApplied() + 1);

                    if(this.storage.getServerLevel().equals(ServerLevel.Leader)) {

                        this.redirectOutput.WriteCout(
                                "[StateMachine] Sending command result back to client..."
                        );

                        // propogate response back to client
                        ClientCommandRPCResultDTO dto = new ClientCommandRPCResultDTO();
                        dto.setSuccess(exitCode == 0);
                        dto.setCommitIndex(this.storage.getCommitIndex());
                        if(exitCode == 0){
                            dto.setMessage("shell command applied successfully ! redirecting output : " + output);
                        }
                        else{
                            dto.setMessage("shell command applied failure ! redirecting output : " + error);
                        }


                        String normalizedMessage = "";
                        for(int i = 0 ;i < dto.getMessage().length(); i++){
                            Character ch = dto.getMessage().charAt(i);
                            if(ch.equals('\"')) {
                                normalizedMessage += "\\\"";
                            }
                            else{
                                normalizedMessage += ch.toString();
                            }
                        }
                        dto.setMessage(normalizedMessage);

                        this.grpc.sendClientCommandResponseRpc(this.clientPort, dto);
                    }

                    Thread.sleep((int)this.timeFragment * 3);
                }catch (Exception ex){
                    System.out.println(ex.getMessage());
                    this.redirectOutput.WriteCerr("[StateMachine ERROR] " + ex.getMessage());
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

    private String readStream(InputStream is) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

}

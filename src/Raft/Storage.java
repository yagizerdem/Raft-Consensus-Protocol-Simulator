package Raft;

import JsonModule.JsonModule;
import Models.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Storage {

    private static final Object fileLock = new Object();
    private JsonModule jsonModule;
    public int serverPort;

    private ServerState serverState;
    public Storage(int serverPort){
        this.serverPort = serverPort;
        this.jsonModule = new JsonModule();
        this.serverState = ServerState.GetDefaultServerState();
    }

    public void initialize() throws Exception {
        String baseUri = "./" + serverPort;
        if(!Files.exists(Path.of(baseUri))) {
            new File(baseUri).mkdirs();
        }
        String serverStateUri = baseUri + "/" + "server-state.txt";
        if(!Files.exists(Path.of(serverStateUri))) {
            PersistentServerState serverState = new PersistentServerState();
            serverState.currentTerm = 0;
            serverState.logs = new ArrayList<>();
            serverState.votedFor = null;
            WritePersistentServerState(serverState);
        }

        String coutUri = baseUri + "/" + "cout.txt";
        if(!Files.exists(Path.of(coutUri))) {
            new File(coutUri).createNewFile();
        }

        String cerrUri = baseUri + "/" +"cerr.txt";

        if(!Files.exists(Path.of(cerrUri))) {
            new File(cerrUri).createNewFile();
        }

    }

    public void WritePersistentServerState(PersistentServerState serverState) throws Exception{
        synchronized (fileLock)  {
            String baseUri = "./" + serverPort;
            String serverStateUri = baseUri + "/" + "server-state.txt";
            String serializedState = jsonModule.Serialize(serverState);
            try (FileOutputStream out = new FileOutputStream(serverStateUri)) {
                out.write(serializedState.getBytes());
            }
        }
    }

    public PersistentServerState ReadPersistentServerState() throws Exception{
        synchronized (fileLock) {
            String baseUri = "./" + serverPort;
            String serverStateUri = baseUri + "/" + "server-state.txt";
            String serializedState = Files.readString(Path.of(serverStateUri));
            PersistentServerState deserialized = jsonModule.Deserialize(serializedState, PersistentServerState.class);
            return deserialized;
        }

    }


    // setters -> persistent states
    public void setCurrentTerm(long term){
        try{
            this.serverState.currentTerm = term;
            this.WritePersistentServerState(this.serverState);

        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void setVotedFor(String votedFor){
        try{
            this.serverState.votedFor = votedFor;
            this.WritePersistentServerState(this.serverState);

        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void appendLog(Log log){
        try{
            this.serverState.logs.add(log);
            this.WritePersistentServerState(this.serverState);
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    // setters -> volatile states

    public void setCommitIndex(int commitIndex) {
        this.serverState.commitIndex = commitIndex;
    }


    public void setLastApplied(int lastApplied) {
        this.serverState.lastApplied = lastApplied;
    }


    public void setNextIndex(ArrayList<Integer> nextIndex) {
        this.serverState.nextIndex = nextIndex;
    }


    public void setMatchIndex(ArrayList<Integer> matchIndex) {
        this.serverState.matchIndex = matchIndex;
    }

    public void setServerLevel(String serverLevel) {
        this.serverState.serverLevel = serverLevel;
    }


    // getters -> persistent states

    public long getCurrentTerm() {
        return this.serverState.currentTerm;
    }

    public String getVotedFor() {
        return this.serverState.votedFor;
    }

    public ArrayList<Log> getLogs() {
        return this.serverState.logs;
    }

    // getters -> volatile states

    public int getCommitIndex() {
        return this.serverState.commitIndex;
    }

    public int getLastApplied() {
        return this.serverState.lastApplied;
    }

    public ArrayList<Integer> getNextIndex() {
        return this.serverState.nextIndex;
    }

    public ArrayList<Integer> getMatchIndex() {
        return this.serverState.matchIndex;
    }

    public String getServerLevel() {
        return this.serverState.serverLevel;
    }


}

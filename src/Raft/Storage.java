package Raft;

import JsonModule.JsonModule;
import Models.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Hashtable;

public class Storage {

    public final Object lock = new Object();
    private static final Object fileLock = new Object();
    private JsonModule jsonModule;
    public int serverPort;

    private ServerState serverState;

    public ArrayList<Log> logs = new ArrayList<>();


    public Storage(int serverPort){
        this.serverPort = serverPort;
        this.jsonModule = new JsonModule();
        this.serverState = ServerState.GetDefaultServerState();
        this.logs = new ArrayList<>();
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

        String logUri = baseUri + "/" + "log.txt";

        if(!Files.exists(Path.of(cerrUri))) {
            new File(logUri).createNewFile();
        }

        // initialize with persistant state
        if(Files.exists(Path.of(serverStateUri))) {
            PersistentServerState persistentServerState = this.ReadPersistentServerState();
            this.setVotedFor(persistentServerState.votedFor);
            this.setCurrentTerm(persistentServerState.currentTerm);
        }

        // initialize with logs
        if(Files.exists(Path.of(logUri))) {
            ArrayList<Log> logs = this.readAllLogs();
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

    public void appendLogEntry(Log log){
        synchronized (fileLock) {
            try{
                String baseUri = "./" + serverPort;
                String logUri = baseUri + "/log.txt";

                // Serialize
                String serialized = String.format(
                        "%d|%d|%s",
                        log.term,
                        log.index,
                        log.shellCommand
                );

                // Append as new line
                Files.write(
                        Path.of(logUri),
                        (serialized + System.lineSeparator()).getBytes(),
                        StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE
                );

                this.logs.add(log);
            }catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public ArrayList<Log> readAllLogs() {
        synchronized (fileLock) {
            ArrayList<Log> logs = new ArrayList<>();
            try {
                String baseUri = "./" + serverPort;
                String logUri = baseUri + "/log.txt";

                Path path = Path.of(logUri);

                if (!Files.exists(path)) {
                    return logs;
                }

                for (String line : Files.readAllLines(path)) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|", 3);
                    if (parts.length < 3) continue;

                    long term = Long.parseLong(parts[0]);
                    int index = Integer.parseInt(parts[1]);
                    String shellCommand = parts[2];

                    Log log = new Log();
                    log.term = term;
                    log.index = index;
                    log.shellCommand = shellCommand;

                    logs.add(log);
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            return logs;
        }
    }

    public void overwriteLogs(ArrayList<Log> logs) {
        synchronized (fileLock) {
            try{
                String baseUri = "./" + serverPort;
                String logUri = baseUri + "/log.txt";

                // overwrite
                new FileOutputStream(logUri).close();

                for(Log log : logs){
                    this.appendLogEntry(log);
                }

            }catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    // setters -> persistent states
    public void setCurrentTerm(long term){
        try{
            this.serverState.currentTerm = term;
            PersistentServerState persistentServerState = new PersistentServerState();
            persistentServerState.votedFor = this.getVotedFor();
            persistentServerState.currentTerm = this.getCurrentTerm();
            this.WritePersistentServerState(persistentServerState);
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void setVotedFor(String votedFor){
        try{
            this.serverState.votedFor = votedFor;
            PersistentServerState persistentServerState = new PersistentServerState();
            persistentServerState.votedFor = this.getVotedFor();
            persistentServerState.currentTerm = this.getCurrentTerm();
            this.WritePersistentServerState(persistentServerState);

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


    public void setNextIndex(Hashtable<Integer, Integer> nextIndex) {
        this.serverState.nextIndex = nextIndex;
    }


    public void setMatchIndex(Hashtable<Integer, Integer> matchIndex) {
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
        return this.logs;
    }

    // getters -> volatile states

    public int getCommitIndex() {
        return this.serverState.commitIndex;
    }

    public int getLastApplied() {
        return this.serverState.lastApplied;
    }

    public Hashtable<Integer, Integer> getNextIndex() {
        return this.serverState.nextIndex;
    }

    public Hashtable<Integer, Integer> getMatchIndex() {
        return this.serverState.matchIndex;
    }

    public String getServerLevel() {
        return this.serverState.serverLevel;
    }



    // log related utils
    public Log getLastLog(){
        if(this.logs.isEmpty()) return  null;
        return  this.logs.get(this.logs.size() -1);
    }
    public Log getLogByIndex(int idx){
        int offset = idx -1;
        if(offset < 0 || offset >= this.logs.size()) return null;
        return  this.logs.get(offset);
    }

}

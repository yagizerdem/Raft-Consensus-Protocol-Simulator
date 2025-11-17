package Models;

import java.util.ArrayList;

public class ServerState {
    // Persistent state on all servers
    public long currentTerm = 0;
    public String votedFor;
    public ArrayList<Log> logs = new ArrayList<>();


    // Volatile state on all servers

    public int commitIndex;
    public int lastApplied;

    //Volatile state on leaders
    public  ArrayList<Integer> nextIndex = new ArrayList<>();
    public  ArrayList<Integer> matchIndex = new ArrayList<>();

}

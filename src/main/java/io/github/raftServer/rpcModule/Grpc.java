package io.github.raftServer.rpcModule;

import io.github.raftServer.jsonModule.*;
import io.github.raftServer.models.*;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;

public class Grpc {

    private final HashMap<Integer, RpcConnection> connections = new HashMap();
    private ServerSocket ss = null;
    private final int serverPort;
    private final JsonModule jsonModule;

    public static class RpcConnection {
        Socket socket;
        DataInputStream inputStream;
        DataOutputStream outputStream;

        public RpcConnection() {}

        public RpcConnection(Socket socket, DataInputStream in, DataOutputStream out) {
            this.socket = socket;
            this.inputStream = in;
            this.outputStream = out;
        }
    }

    public Grpc(int serverPort) {
        this.serverPort = serverPort;
        this.jsonModule = new JsonModule();
    }

    public void start() {
        try {
            this.ss = new ServerSocket(serverPort);
            acceptInNewThread();
        }catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void acceptInNewThread() {
        new Thread(() -> {
            try {
                Socket s  = ss.accept();
                RpcConnection connection = new RpcConnection(s,
                        new DataInputStream(
                                new BufferedInputStream(s.getInputStream())),
                        new DataOutputStream(
                                new BufferedOutputStream(s.getOutputStream())));
                connections.put(s.getPort(), connection);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        }).start();
    }


}

package io.github.raftServer.rpcModule;

import io.github.raftServer.jsonModule.*;
import io.github.raftServer.models.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Grpc implements IRpcHandler {

    // used to read data
    private final List<RpcConnection> incomingConnections = Collections.synchronizedList(new ArrayList<>());
    // used to send data
    private final List<RpcConnection> outConnections = Collections.synchronizedList(new ArrayList<>());

    private ServerSocket ss = null;
    private final int serverPort;
    public List<Peer> peers = new ArrayList<>();
    private final JsonModule jsonModule;
    private final String nodeId;

    // compares by giving dest port NOT LOCAL PORT
    private boolean containsPort(List<RpcConnection> connections, int port) {
        for(RpcConnection conn : connections) {
            if(conn.socket.getPort() == port) return true;
        }
        return false;
    }

    private boolean containsNodeId(List<RpcConnection> connections, String nodeId) {
        for(RpcConnection conn : connections) {
            if(conn.nodeId.equals(nodeId)) return true;
        }
        return false;
    }

    public static class RpcConnection {
        Socket socket; // client socket
        DataInputStream inputStream;
        DataOutputStream outputStream;
        String nodeId;

        public RpcConnection() {}

        public RpcConnection(Socket socket, DataInputStream in, DataOutputStream out) {
            this.socket = socket;
            this.inputStream = in;
            this.outputStream = out;
        }

        public RpcConnection(Socket socket, DataInputStream in, DataOutputStream out, String nodeId) {
            this.socket = socket;
            this.inputStream = in;
            this.outputStream = out;
            this.nodeId = nodeId;
        }

        public boolean isUsable() {
            return socket != null
                    && socket.isConnected()
                    && !socket.isClosed()
                    && !socket.isInputShutdown()
                    && !socket.isOutputShutdown();
        }
    }

    public static record Peer(String nodeId, String host, int port) {}

    public int getServerPort() {
        return this.serverPort;
    }

    public Grpc(int serverPort, String nodeId) {
        this.serverPort = serverPort;
        this.jsonModule = new JsonModule();
        this.nodeId = nodeId;
    }

    public Grpc(int serverPort, List<Peer> peers, String nodeId) {
        this.serverPort = serverPort;
        this.peers = peers;
        this.jsonModule = new JsonModule();
        this.nodeId = nodeId;
    }

    public void start() {
        try {
            this.ss = new ServerSocket(serverPort);
            acceptInNewThread();
            connectPeers();
            peerConnectionsDebug();
        }catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void acceptInNewThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket s = ss.accept();
                    RpcConnection connection = new RpcConnection(
                            s,
                            new DataInputStream(new BufferedInputStream(s.getInputStream())),
                            new DataOutputStream(new BufferedOutputStream(s.getOutputStream()))
                    );
                    readIncomingMessageInNewThread(connection);
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }).start();
    }

    public void connectPeers() {
        new Thread(() -> {
            while (true) {
                try {
                    for(Peer peer : peers) {
                     new Thread(() -> {
                         try {
                             synchronized (outConnections) {
                                 if(containsPort(outConnections, peer.port)) return;
                             }
                             Socket clientSocket = new Socket(peer.host, peer.port);
                             synchronized (outConnections) {
                                 if(containsPort(outConnections, peer.port)) {
                                     clientSocket.close();
                                     return;
                                 }
                                 RpcConnection conn = new RpcConnection(clientSocket,
                                         new DataInputStream(clientSocket.getInputStream()),
                                         new DataOutputStream(clientSocket.getOutputStream()),
                                         peer.nodeId);
                                 outConnections.add(conn);
                                 // System.out.println("Connected to localhost:" + peer);
                             }
                         } catch (IOException ex) {
                             System.err.println(ex.getMessage());
                         }
                     }).start();
                    }
                    Thread.sleep(500);

                }catch (InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }).start();
    }

    public void peerConnectionsDebug() {
        if(serverPort != 8001) return;
        new Thread(() -> {
            while (true) {
                try {
                    System.out.println("Incoming connections : ");
                    for(RpcConnection con : incomingConnections) {
                        System.out.println("\t - HOST : " + con.socket.getInetAddress().getHostName()  + " - PORT : " + con.socket.getPort());
                    }
                    System.out.println("Out connections : ");
                    for(RpcConnection con : outConnections) {
                        System.out.println("\t - HOST : " + con.socket.getInetAddress().getHostName()  + " - PORT : " + con.socket.getPort());
                    }
                    System.out.println("-".repeat(50));
                    Thread.sleep(2000);
                }catch (InterruptedException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }).start();
    }

    private void readIncomingMessageInNewThread(RpcConnection connection) {
        new Thread(() -> {
            try {
                while (true) {
                    String message = connection.inputStream.readUTF();
                    try {
                        RpcPayload payload = jsonModule.Deserialize(message, RpcPayload.class);
                        if(payload.type.equals(RpcTypes.ServerCredentialsRpc)) {
                            handleServerCredentialsRpc(connection, payload.serverCredentialsDTO);
                            continue;
                        }

                        if(connection.nodeId == null) {
                            System.err.println("Unauthenticated connection");
                            closeQuietly(connection);
                            return;
                        }
                        if(!incomingConnections.contains(connection)) {
                            System.err.println("Unauthenticated connection");
                            closeQuietly(connection);
                            return;
                        }


                    }catch (JsonModule.JsonSerializationException ex) {
                        System.err.println("Invalid json format send to server : " + nodeId + " message : " + ex.getMessage());
                    }
                    catch (Exception ex) {
                        System.err.println("Error occured in node : " + nodeId + " while receiving data. message : " + ex.getMessage());
                    }
                }
            } catch (IOException ex) {
                incomingConnections.remove(connection);
                closeQuietly(connection);
                System.err.println("Incoming connection closed: " + ex.getMessage());
            }

        }).start();
    }

    private void closeQuietly(RpcConnection conn) {
        try {
            if (conn.inputStream != null) conn.inputStream.close();
        } catch (IOException ignored) {}

        try {
            if (conn.outputStream != null) conn.outputStream.close();
        } catch (IOException ignored) {}

        try {
            if (conn.socket != null && !conn.socket.isClosed()) {
                conn.socket.close();
            }
        } catch (IOException ignored) {}
    }

    private void sendRpc(RpcConnection conn, String data) {
        try {
            conn.outputStream.writeUTF(data);
        }catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    // helper methods for sending rpc
    public void sendServerCredentialsRpc(String nodeId, ServerCredentialsDTO dto) {
        try {
            RpcPayload payload = new RpcPayload();
            payload.serverCredentialsDTO = dto;
            payload.type = RpcTypes.ServerCredentialsRpc;
            String json = jsonModule.Serialize(payload);
            Optional<RpcConnection> option = outConnections.stream().filter(x -> x.nodeId.equals(nodeId)).findFirst().stream().findFirst();
            if(option.isPresent()) {
                RpcConnection conn = option.get();
                sendRpc(conn, json);
            }
        } catch (Exception ex) {
            System.out.println("sendServerCredentialsRpc error: " + ex.getMessage());
        }
    }

    @Override
    public void handleRequestVoteRpc(RpcConnection connection, RequestVoteRPCDTO requestVoteDto) {
    }

    @Override
    public void handleRequestVoteResponseRpc(RpcConnection connection, RequestVoteResultRPCDTO requestVoteResponseDto) {
    }

    @Override
    public void handleAppendEntriesRpc(RpcConnection connection, AppendEntriesRPCDTO appendEntriesDto) {
    }

    @Override
    public void handleAppendEntriesResponseRpc(RpcConnection connection, AppendEntriesRPCResultDTO appendEntriesResponseDto) {
    }

    @Override
    public void handleClientCommandRpc(RpcConnection connection, ClientCommandRPCDTO clientCommandDto) {
    }

    @Override
    public void handleClientCommandResponseRpc(RpcConnection connection, ClientCommandRPCResultDTO clientCommandRPCResultDTO) {
    }

    @Override
    public void handleServerCredentialsRpc(RpcConnection connection, ServerCredentialsDTO serverCredentialsDTO) {
        synchronized (incomingConnections) {
            RpcConnection old = incomingConnections.stream()
                    .filter(c -> c != connection)
                    .filter(c -> serverCredentialsDTO.nodeId.equals(c.nodeId))
                    .findFirst()
                    .orElse(null);

            if (old != null) {
                incomingConnections.remove(old);
                closeQuietly(old);
            }

            connection.nodeId = serverCredentialsDTO.nodeId;

            if (!incomingConnections.contains(connection)) {
                incomingConnections.add(connection);
            }
        }
    }

    @Override
    public void handleServerCredentialsResponseRpc(RpcConnection connection, ServerCredentialsResponseDTO serverCredentialsResponseDTO) {

    }
}

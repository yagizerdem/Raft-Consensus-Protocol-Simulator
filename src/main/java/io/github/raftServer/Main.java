package io.github.raftServer;

import io.github.raftServer.raft.RaftServer;
import io.github.raftServer.rpcModule.Grpc;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String PORT_ENV = System.getenv().getOrDefault("PORT", null);
        String PEERS = System.getenv().getOrDefault("PEERS", null);
        String NODE_ID = System.getenv().getOrDefault("NODE_ID", null);
        if(PORT_ENV == null) {
            System.err.println("Port number is not given to raft-node");
            System.exit(1);
        }
        if(!isInteger(PORT_ENV)) {
            System.err.println("Port number should be an integer");
            System.exit(1);
        }
        if(PEERS == null) {
            System.err.println("PEER port numbers should be integer");
            System.exit(1);
        }
        if(NODE_ID == null) {
            System.err.println("NODE_ID should be string");
            System.exit(1);
        }
        for (String peer : PEERS.split(",")) {
            peer = peer.trim();
            if (peer.isEmpty()) {
                continue;
            }
            String[] parts = peer.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid peer format: " + peer);
            }
            String nodeId = parts[0].trim();
            String host = parts[1].trim();
            String port = parts[2].trim();
            if (nodeId.isEmpty()) {
                throw new IllegalArgumentException("NodeId cannot be empty: " + peer);
            }
            if (host.isEmpty()) {
                throw new IllegalArgumentException("Peer host cannot be empty: " + peer);
            }
            if (!isInteger(port)) {
                throw new IllegalArgumentException("Invalid peer port: " + peer);
            }
        }


        int port = Integer.parseInt(PORT_ENV);
        List<Grpc.Peer> peers = Arrays.stream(PEERS.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(peer -> {
                    String[] parts = peer.split(":");
                    return new Grpc.Peer(
                            parts[0].trim(), // node id
                            parts[1].trim(), // host
                            Integer.parseInt(parts[2].trim()) // port
                    );
                })
                .toList();

        RaftServer server = new RaftServer(port, peers, NODE_ID);
        server.start();

    }

    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
}
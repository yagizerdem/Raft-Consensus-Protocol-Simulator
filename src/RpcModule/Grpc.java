package RpcModule;

import JsonModule.JsonModule;
import Models.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Grpc {

    private JsonModule jsonModule;
    private ServerSocket ss = null;
    private IRpcHandler rpcHandlers;

    public Grpc(int port, IRpcHandler rpcHandlers){
        try{
            this.rpcHandlers = rpcHandlers;
            jsonModule = new JsonModule();
            ss = new ServerSocket(port);
            startNetworkListener();
        }catch (IOException ex){
            System.out.println(ex.getMessage());
        }
    }

    public void startNetworkListener(){
        new Thread(() -> {
            try{
                while (true) {
                    Socket client = ss.accept();
                    handleRpcInNewThread(client);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }).start();
    }

    // send rpc

    public void sendRpc(int targetPort, String paylaod) {
        try (Socket s = new Socket("localhost", targetPort)) {
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeUTF(paylaod);
            out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // helper methods for sending rpc
    public void sendAppendEntriesRpc(int targetPort, AppendEntriesRPCDTO dto) {
        try {
            RpcPayload payload = new RpcPayload();
            payload.appendEntriesRPCDTO = dto;
            payload.type = RpcTypes.AppendEntriesRpc;
            String json = jsonModule.Serialize(payload);
            sendRpc(targetPort, json);
        } catch (Exception ex) {
            System.out.println("sendAppendEntriesRpc error: " + ex.getMessage());
        }
    }

    public void sendAppendEntriesResponseRpc(int targetPort, AppendEntriesRPCResultDTO dto) {
        try {
            RpcPayload payload = new RpcPayload();
            payload.appendEntriesRPCResultDTO = dto;
            payload.type = RpcTypes.AppendEntriesResponseRpc;
            String json = jsonModule.Serialize(payload);
            sendRpc(targetPort, json);
        } catch (Exception ex) {
            System.out.println("sendAppendEntriesResponseRpc error: " + ex.getMessage());
        }
    }

    public void sendRequestVoteRpc(int targetPort, RequestVoteRPCDTO dto) {
        try {
            RpcPayload payload = new RpcPayload();
            payload.requestVoteRPCDTO = dto;
            payload.type = RpcTypes.RequestVoteRpc;
            String json = jsonModule.Serialize(payload);
            sendRpc(targetPort, json);
        } catch (Exception ex) {
            System.out.println("sendRequestVoteRpc error: " + ex.getMessage());
        }
    }

    public void sendRequestVoteResponseRpc(int targetPort, RequestVoteResultRPCDTO dto) {
        try {
            RpcPayload payload = new RpcPayload();
            payload.requestVoteResultRPCDTO = dto;
            payload.type = RpcTypes.RequestVoteResponseRpc;
            String json = jsonModule.Serialize(payload);
            sendRpc(targetPort, json);
        } catch (Exception ex) {
            System.out.println("sendRequestVoteResponseRpc error: " + ex.getMessage());
        }
    }

    public void  sendClientCommandRcp(int targetPort, ClientCommandRPCDTO dto){
        try {
            RpcPayload payload = new RpcPayload();
            payload.clientCommandRPCDTO = dto;
            payload.type = RpcTypes.ClientCommandRpc;
            String json = jsonModule.Serialize(payload);
            sendRpc(targetPort, json);
        } catch (Exception ex) {
            System.out.println("sendClientCommandRcp error: " + ex.getMessage());
        }
    }



    // rpc handlers

    public void handleRpcInNewThread(Socket client){
        new Thread(() ->{
            handleRpc(client);
        }).start();
    }

    public void handleRpc(Socket s) {
        try (DataInputStream in = new DataInputStream(s.getInputStream())) {
            String serializedMessage = in.readUTF();

            RpcPayload payload = jsonModule.Deserialize(serializedMessage, RpcPayload.class);


            if (payload.type.equals(RpcTypes.RequestVoteRpc)) {
                this.rpcHandlers.handleRequestVoteRpc(payload.requestVoteRPCDTO);
                return;
            }

            if (payload.type.equals(RpcTypes.RequestVoteResponseRpc)) {
                this.rpcHandlers.handleRequestVoteResponseRpc(payload.requestVoteResultRPCDTO);
                return;
            }

            if (payload.type.equals(RpcTypes.AppendEntriesRpc)) {
                this.rpcHandlers.handleAppendEntriesRpc(payload.appendEntriesRPCDTO);
                return;
            }

            if (payload.type.equals(RpcTypes.AppendEntriesResponseRpc)) {
                this.rpcHandlers.handleAppendEntriesResponseRpc(payload.appendEntriesRPCResultDTO);
                return;
            }

            if(payload.type.equals(RpcTypes.ClientCommandRpc)) {
                this.rpcHandlers.handleClientCommandRpc(payload.clientCommandRPCDTO);
                return;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

import Models.*;
import Raft.RaftModule;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;

import javax.sound.midi.Soundbank;
import java.awt.event.TextEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ClientShell {

    private static Grpc grpc;
    private static int port;
    private static ArrayList<Integer> peers;

    public static void main(String[] args)  {

        port = 8000;
        grpc = new Grpc(port, new IRpcHandler() {
            @Override
            public void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void handleClientCommandRpc(ClientCommandRPCDTO clientCommandRpcDto){
                throw new UnsupportedOperationException();
            }
        });
        peers = new ArrayList<>();

        for(int i = 0; i < args.length; i++){
            peers.add(Integer.valueOf(args[i]));
        }

        TextEditor();
    }

    public static void TextEditor() {
        System.out.println("Please enter the shell command you want to send to the leader server:");

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    // send grpc to leader server
                    for(Integer peer : peers){
                        ClientCommandRPCDTO commandDto = new ClientCommandRPCDTO();
                        commandDto.shellCommand = line.trim();
                        commandDto.clientPort = port;
                        grpc.sendClientCommandRcp(peer, commandDto);
                    }
                }

            } catch (Exception e) {
                System.out.println("Error reading input: " + e.getMessage());
            }
        }).start();
    }

}

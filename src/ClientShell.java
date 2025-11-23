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

    private static boolean waitResponse = false;

    public static void main(String[] args)  {

        port = Integer.valueOf(args[0]);

        System.out.println(port);

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

            @Override
            public void handleClientCommandResponseRpc(ClientCommandRPCResultDTO clientCommandRPCResultDTO) {
                ClientShell.handleClientCommandResponseRpc(clientCommandRPCResultDTO);
            }


        });
        peers = new ArrayList<>();

        for(int i = 1; i < args.length; i++){
            peers.add(Integer.valueOf(args[i]));
        }

        TextEditor();
    }

    public static void TextEditor() {
        System.out.println("#".repeat(50));
        System.out.println("Client replicated shell");
        System.out.println("#".repeat(50));
        System.out.println("Please enter the shell command you want to send to the leader server:");

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    if(waitResponse) continue;

                    // send grpc to leader server
                    for(Integer peer : peers){
                        ClientCommandRPCDTO commandDto = new ClientCommandRPCDTO();
                        commandDto.shellCommand = line.trim();
                        commandDto.clientPort = port;
                        grpc.sendClientCommandRcp(peer, commandDto);
                    }
                    waitResponse = true;
                    System.out.println("Wait for server processing command");
                }

            } catch (Exception e) {
                System.out.println("Error reading input: " + e.getMessage());
            }
        }).start();
    }


    public static void handleClientCommandResponseRpc(ClientCommandRPCResultDTO dto){
        waitResponse = false;
        System.out.println(String.format("commit index : %s | leader server response : %s ", dto.getCommitIndex()
                , dto.getMessage()));
    }
}

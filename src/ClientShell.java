import Models.*;
import Raft.RaftModule;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;

import javax.sound.midi.Soundbank;
import java.awt.event.TextEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientShell {

    private static Grpc grpc;
    private static int port;
    private static ArrayList<Integer> peers;
    private static boolean waitResponse = false;
    private static ArrayList<String> defaultShellCommands = new ArrayList<>();

    private static int defaultShellCommandIdx = 0;

    public static void main(String[] args) {

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
            public void handleClientCommandRpc(ClientCommandRPCDTO clientCommandRpcDto) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void handleClientCommandResponseRpc(ClientCommandRPCResultDTO clientCommandRPCResultDTO) {
                ClientShell.handleClientCommandResponseRpc(clientCommandRPCResultDTO);
            }


        });
        peers = new ArrayList<>();

        String peersArg = null;
        String defaultInput = null;

        for (String arg : args) {
            if (arg.startsWith("-peers=")) {
                peersArg = arg.substring("-peers=".length());
            }
            if (arg.startsWith("-defaultInput=")) {
                defaultInput = arg.substring("-defaultInput=".length());
            }
        }

        if (peersArg != null && peersArg.length() > 0) {
            for (String s : peersArg.split(",")) {
                peers.add(Integer.valueOf(s.trim()));
            }
        }


        try {
            Path path = Paths.get(defaultInput);
            String content = Files.readString(path);
            defaultShellCommands = new ArrayList<>(Arrays.stream(content.split("\n")).toList().stream().filter(x -> x.trim() != "" || x != null).toList());
        } catch (Exception ex) {
            System.out.println("[Exception] : " + ex.getMessage());
        }


        TextEditor();
    }

    public static void TextEditor() {
        System.out.println("#".repeat(50));
        System.out.println("Client replicated shell");
        System.out.println("#".repeat(50));
        System.out.println("Please enter the shell command you want to send to the leader server:");

        if (!defaultShellCommands.isEmpty()) {
            System.out.println("Default input file found executing default inputs !");
        }

        while (defaultShellCommandIdx < defaultShellCommands.size()) {
            try {
                Thread.sleep(50);
                String shellCommand = defaultShellCommands.get(defaultShellCommandIdx);
                if (waitResponse) continue;

                System.out.println("Shell command to execute : " + shellCommand);
                waitResponse = true;

                for (Integer peer : peers) {
                    ClientCommandRPCDTO commandDto = new ClientCommandRPCDTO();
                    commandDto.shellCommand = shellCommand.trim();
                    commandDto.clientPort = port;
                    grpc.sendClientCommandRcp(peer, commandDto);
                }

                System.out.println("Wait for server processing command");
                defaultShellCommandIdx += 1;
            }
            catch (Exception ex) {
                System.out.println("[Exception] : " + ex.getMessage());
            }

        }

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    if(waitResponse) continue;

                    String shellCommand = line.trim();

                    waitResponse = true;

                    // send grpc to leader server
                    for(Integer peer : peers){
                        ClientCommandRPCDTO commandDto = new ClientCommandRPCDTO();
                        commandDto.shellCommand = shellCommand;
                        commandDto.clientPort = port;
                        grpc.sendClientCommandRcp(peer, commandDto);
                    }

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

        System.out.println("-".repeat(20));
    }
}

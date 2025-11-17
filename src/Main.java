import JsonModule.JsonModule;
import Models.*;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;
import RpcModule.RpcTypes;
import  JsonModule.*;

public class Main {

    public static void main(String[] args) {
        try{
            int serverPort = Integer.parseInt(args[0]);
            int i = 0;



            Grpc grpc = new Grpc(serverPort, new IRpcHandler() {
                @Override
                public void handleRequestVoteRpc(RequestVoteRPCDTO requestVoteDto) {
                    System.out.println(requestVoteDto);
                }

                @Override
                public void handleRequestVoteResponseRpc(RequestVoteResultRPCDTO requestVoteResponseDto) {
                    System.out.println(requestVoteResponseDto);
                }

                @Override
                public void handleAppendEntriesRpc(AppendEntriesRPCDTO appendEntriesDto) {
                    System.out.println(appendEntriesDto);
                }

                @Override
                public void handleAppendEntriesResponseRpc(AppendEntriesRPCResultDTO appendEntriesResponseDto) {
                    System.out.println(appendEntriesResponseDto);
                }
            });

            while (true) {

                if(i > 0 && i % 5 == 0) {
                    AppendEntriesRPCDTO dto = new AppendEntriesRPCDTO();
                    dto.term = 10;
                    dto.leaderCommit = 1;
                    dto.prevLogIndex = 24;
                    if(serverPort != 9001) {
                        grpc.sendAppendEntriesRpc(9001, dto);
                    }
                }

                Thread.sleep(200);
                i++;
            }


        }catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }


    }


}
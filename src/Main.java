import JsonModule.JsonModule;
import Models.*;
import Raft.RaftModule;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;
import RpcModule.RpcTypes;
import  JsonModule.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        try{
            ArrayList<Integer> peers = new ArrayList<>(
                    Arrays.stream(Arrays.copyOfRange(args, 1 , args.length -1))
                    .map(x -> Integer.parseInt(x)).collect(Collectors.toList()));

            int leaderPort = Integer.valueOf(args[args.length -1]);
            System.out.println(leaderPort);

            int serverPort = Integer.parseInt(args[0]);
            RaftModule raftModule = new RaftModule(serverPort, peers, leaderPort == serverPort);

            raftModule.Start();

        }catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }


    }


}
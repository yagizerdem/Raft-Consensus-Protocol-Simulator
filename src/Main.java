import JsonModule.JsonModule;
import Models.*;
import Raft.RaftModule;
import RpcModule.Grpc;
import RpcModule.IRpcHandler;
import RpcModule.RpcTypes;
import  JsonModule.*;

public class Main {

    public static void main(String[] args) {
        try{

            int serverPort = Integer.parseInt(args[0]);
            RaftModule raftModule = new RaftModule(serverPort);

            raftModule.Start();

        }catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }


    }


}
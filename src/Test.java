import JsonModule.JsonModule;
import Models.ClientCommandRPCDTO;
import Models.RpcPayload;
import RpcModule.Grpc;
import RpcModule.RpcTypes;

public class Test {

    public static void main(String[] args) throws Exception {

        JsonModule jsonModule = new JsonModule();
        String cmd = "type nul > \\\"test test2.txt\\\"";
        ClientCommandRPCDTO dto = new ClientCommandRPCDTO();
        dto.shellCommand = cmd;
        dto.clientPort = 3000;


        RpcPayload p = new RpcPayload();
        p.type = RpcTypes.ClientCommandRpc;
        p.clientCommandRPCDTO = dto;

        String serialized = jsonModule.Serialize(p);
        System.out.println(serialized);


        RpcPayload desserialize = jsonModule.Deserialize(serialized, RpcPayload.class);
        System.out.println(desserialize);

    }
}

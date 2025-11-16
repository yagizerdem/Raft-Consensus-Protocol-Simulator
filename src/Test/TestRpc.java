package  Test;

import java.io.File;

public class TestRpc {

    public static void main(String[] args) throws Exception {


        int numServers = 2;
        int basePort = 9000;

        String classpath = new File("out/production/Raft").getAbsolutePath();

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp", classpath,
                    "Main",
                    String.valueOf(port)
            );

            File serverDir = new File("./server" + i);
            if (!serverDir.exists()) serverDir.mkdirs();

            pb.directory(serverDir);

            pb.redirectOutput(new File(serverDir,"stdout.log"));
            pb.redirectError(new File(serverDir,"stderr.log"));


            Process p =  pb.start();
            int status  = p.waitFor();
        }
    }
}
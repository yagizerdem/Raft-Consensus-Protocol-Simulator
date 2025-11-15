import java.io.File;

public class Launcher {

    public static void main(String[] args) throws Exception {


        int numServers = 5;
        int basePort = 9000;

        for (int i = 0; i < numServers; i++) {
            int port = basePort + i;



            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp", "out/production/Raft/Main",
                    "raft.RaftLauncher",
                    String.valueOf(port)
            );

            pb.directory(new File("server" + i));

            pb.start();
        }
    }
}
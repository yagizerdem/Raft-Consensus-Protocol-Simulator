package Raft;

import java.io.FileOutputStream;
import java.io.FileWriter;

public class RedirectOutput {
    public int serverPort;
    private String coutUri;
    private String cerrUri;
    private FileWriter fcoutStream;
    private FileWriter ferrStream;

    public RedirectOutput(int serverPort){
        this.serverPort = serverPort;
    }

    public void initialize() {
        try{
            coutUri = "./"+ this.serverPort + "/" + "cout.txt";
            cerrUri = "./" + this.serverPort + "/" + "cerr.txt";

            // overwrite existing files
            new FileOutputStream(coutUri, false).close();
            new FileOutputStream(cerrUri, false).close();


            fcoutStream = new FileWriter(coutUri, true);
            ferrStream = new FileWriter(cerrUri, true);
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void WriteCout(String text){
        try{
            this.fcoutStream.write("[" + timestamp() + "] " + text + "\n");
            this.fcoutStream.flush();
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void WriteCerr(String text){
        try{
            this.ferrStream.write("[" + timestamp() + "] " + text + "\n");
            this.ferrStream.flush();
        }catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    private String timestamp() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}

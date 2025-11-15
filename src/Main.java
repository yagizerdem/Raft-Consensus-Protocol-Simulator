import Rpc.RequestVoteRPCDTO;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {


        try{

            RequestVoteRPCDTO dto = new RequestVoteRPCDTO();
            dto.term = 10;

            System.out.println("main java file executed");

            System.out.println(dto);

        }catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }


    }


}
package io.github.raftServer;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        String PORT_ENV = System.getenv().getOrDefault("PORT", null);
        if(PORT_ENV == null) {
            System.err.println("Port number is not given to raft-node");
            System.exit(1);
        }
        if(!isInteger(PORT_ENV)) {
            System.err.println("Port number should be an integer");
            System.exit(1);
        }
        int PORT = Integer.parseInt(PORT_ENV);
        System.out.println(PORT);
    }

    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
}
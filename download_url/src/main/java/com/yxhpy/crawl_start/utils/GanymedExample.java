package com.yxhpy.crawl_start.utils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GanymedExample {
    public static String executeCommand(String hostname, String username, String password, String command) throws Exception {
        StringBuilder output = new StringBuilder();
        Connection conn = new Connection(hostname);
        try {
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            if (isAuthenticated) {
                Session session = conn.openSession();
                session.execCommand(command);
                InputStream stdout = new StreamGobbler(session.getStdout());
                BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
                session.close();
            }
        } finally {
            conn.close();
        }
        return output.toString();
    }

}

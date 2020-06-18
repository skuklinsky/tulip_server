import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;

import java.io.IOException;
import java.util.*;


class RunServer {

    private static int LISTENING_PORT = 16042;

    public static void main(String[] args) {

        Global global = ReadWrite.readGlobalFromFile("globalObjectAsFile");
        LoginInfo loginInfo = ReadWrite.readLoginInfoFromFile("loginInfoObjectAsFile");

        if (global == null) {
            global = new Global();
            System.out.println("New Global variable instantiated");
        } else {
            System.out.println("Successfully loaded old Global instance from file");
        }

        if (loginInfo == null) {
            loginInfo = new LoginInfo();
            System.out.println("New LoginInfo variable instantiated");
        } else {
            System.out.println("Successfully loaded old LoginInfo instance from file");
        }

        HashSet<OutputStream> setOfActiveOutputStreams = new HashSet<>();

        ServerSocket serverSocket;
        Socket connectionSocket;
        ConnectionThread connectionThread;

        try {
            serverSocket = new ServerSocket(LISTENING_PORT);
            System.out.println("Successfully listening on port " + LISTENING_PORT);
        } catch (IOException e) {
            System.out.println("Failed to establish a listening socket");
            return;
        }

        while (true) {
            try {
                connectionSocket = serverSocket.accept();
                connectionThread = new ConnectionThread(connectionSocket, global, loginInfo, setOfActiveOutputStreams);
                connectionThread.start();
            } catch (IOException e) {
                System.out.println("Error establishing a connection");
            }
        }
    }
}